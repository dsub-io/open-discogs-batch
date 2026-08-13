package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.records.LabelUrlRecord;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class ConvergingRelationItemWriterIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final int BATCH_SIZE = 5;
  private static final String DELETE_GUARD_FUNCTION = "reject_new_relation_root_delete";
  private static final String DELETE_GUARD_TRIGGER = "guard_new_relation_root_delete";

  private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

  @BeforeAll
  static void migrateDatabase() throws Exception {
    boolean tableExists;
    try (Connection connection = dataSource.getConnection();
        ResultSet tables =
            connection
                .getMetaData()
                .getTables(null, "public", "label_url", new String[] {"TABLE"})) {
      tableExists = tables.next();
    }
    if (!tableExists) {
      new ResourceDatabasePopulator(
              new ClassPathResource("migrations/V001__initial_schema.sql"))
          .execute(dataSource);
    }
    if (!columnExists("ordinal")) {
      new ResourceDatabasePopulator(
              new ClassPathResource("migrations/V023__label_url_ordinal.sql"))
          .execute(dataSource);
    }
    if (columnExists("created_at")) {
      new ResourceDatabasePopulator(
              new ClassPathResource("migrations/V039__remove_relation_created_at.sql"))
          .execute(dataSource);
    }
  }

  private static boolean columnExists(String columnName) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        ResultSet columns =
            connection.getMetaData().getColumns(null, "public", "label_url", columnName)) {
      return columns.next();
    }
  }

  @BeforeEach
  void clearState() {
    jdbcTemplate.execute("truncate table label restart identity cascade");
    jdbcTemplate.update(
        """
        insert into label (id, created_at, last_modified_at, name)
        values (1, now(), now(), 'label')
        """);
  }

  @Test
  void keepsCurrentPhysicalRowAndDeletesOnlyStaleKeys() throws Exception {
    String currentUrl = "https://current.example";
    String staleUrl = "https://stale.example";
    jdbcTemplate.update(
        """
        insert into label_url
            (id, last_modified_at, hash, url, label_id)
        values
            (10, now(), ?, ?, 1),
            (11, now(), ?, ?, 1)
        """,
        currentUrl.hashCode(),
        currentUrl,
        staleUrl.hashCode(),
        staleUrl);

    writer().write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.LABEL,
                    1,
                    List.of(labelUrl(1, currentUrl))))));

    assertThat(
            jdbcTemplate.queryForList(
                "select id from label_url order by id", Integer.class))
        .containsExactly(10);
  }

  @Test
  void emptyRelationSetDeletesEveryPriorRelationForTheRoot() throws Exception {
    jdbcTemplate.update(
        """
        insert into label_url
            (last_modified_at, hash, url, label_id)
        values (now(), 1, 'https://stale.example', 1)
        """);

    writer().write(
        new Chunk<>(List.of(new RelationSet(EntityType.LABEL, 1, List.of()))));

    assertThat(jdbcTemplate.queryForObject("select count(*) from label_url", Long.class))
        .isZero();
  }

  @Test
  void newRelationRootDoesNotIssueAStaleDelete() throws Exception {
    String url = "https://new.example";
    jdbcTemplate.execute(
        """
        create function reject_new_relation_root_delete() returns trigger
        language plpgsql as $$
        begin
          raise exception 'new relation root must not issue delete';
        end;
        $$
        """);
    jdbcTemplate.execute(
        """
        create trigger guard_new_relation_root_delete
        before delete on label_url
        for each statement execute function reject_new_relation_root_delete()
        """);

    try {
      writer().write(
          new Chunk<>(
              List.of(
                  new RelationSet(
                      EntityType.LABEL,
                      1,
                      List.of(labelUrl(1, url))))));
    } finally {
      jdbcTemplate.execute("drop trigger " + DELETE_GUARD_TRIGGER + " on label_url");
      jdbcTemplate.execute("drop function " + DELETE_GUARD_FUNCTION + "()");
    }

    assertThat(jdbcTemplate.queryForObject("select count(*) from label_url", Long.class))
        .isOne();
  }

  @Test
  void refreshesOrdinalAndSkipsAnUnchangedRetry() throws Exception {
    String url = "https://ordinal.example";
    LocalDateTime firstObservation = LocalDateTime.of(2026, 8, 1, 0, 0);
    writer().write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.LABEL,
                    1,
                    List.of(labelUrl(1, url, 5, firstObservation))))));

    LocalDateTime secondObservation = firstObservation.plusDays(1);
    RelationSet reordered =
        new RelationSet(
            EntityType.LABEL,
            1,
            List.of(labelUrl(1, url, 1, secondObservation)));
    writer().write(new Chunk<>(List.of(reordered)));

    assertThat(
            jdbcTemplate.queryForObject(
                "select ordinal from label_url where label_id = 1", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select last_modified_at from label_url where label_id = 1",
                LocalDateTime.class))
        .isEqualTo(secondObservation);

    jdbcTemplate.execute(
        """
        create function reject_unchanged_ordinal_update() returns trigger
        language plpgsql as $$
        begin
          raise exception 'unchanged ordinal must not update';
        end;
        $$
        """);
    jdbcTemplate.execute(
        """
        create trigger reject_unchanged_ordinal_update
        before update on label_url
        for each row execute function reject_unchanged_ordinal_update()
        """);
    try {
      writer().write(new Chunk<>(List.of(reordered)));
    } finally {
      jdbcTemplate.execute("drop trigger reject_unchanged_ordinal_update on label_url");
      jdbcTemplate.execute("drop function reject_unchanged_ordinal_update()");
    }
  }

  private ItemWriter<RelationSet> writer() {
    DSLContext context = DSL.using(dataSource, SQLDialect.POSTGRES);
    ItemWriter<UpdatableRecord<?>> records = new DefaultLJooqItemWriter<>(context);
    ItemWriter<Collection<UpdatableRecord<?>>> batches =
        new CollectionItemWriter<>(records, BATCH_SIZE);
    return new ConvergingRelationItemWriter(dataSource, batches);
  }

  private LabelUrlRecord labelUrl(int labelId, String url) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return labelUrl(labelId, url, 0, now);
  }

  private LabelUrlRecord labelUrl(
      int labelId, String url, int ordinal, LocalDateTime observedAt) {
    return new LabelUrlRecord()
        .setLabelId(labelId)
        .setUrl(url)
        .setHash(url.hashCode())
        .setOrdinal(ordinal)
        .setLastModifiedAt(observedAt);
  }
}
