package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.records.LabelUrlRecord;
import java.sql.Connection;
import java.sql.ResultSet;
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
    try (Connection connection = dataSource.getConnection();
        ResultSet tables =
            connection
                .getMetaData()
                .getTables(null, "public", "label_url", new String[] {"TABLE"})) {
      if (tables.next()) {
        return;
      }
    }
    new ResourceDatabasePopulator(
            new ClassPathResource("migrations/V001__initial_schema.sql"))
        .execute(dataSource);
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
            (id, created_at, last_modified_at, hash, url, label_id)
        values
            (10, now(), now(), ?, ?, 1),
            (11, now(), now(), ?, ?, 1)
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
            (created_at, last_modified_at, hash, url, label_id)
        values (now(), now(), 1, 'https://stale.example', 1)
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

  private ItemWriter<RelationSet> writer() {
    DSLContext context = DSL.using(dataSource, SQLDialect.POSTGRES);
    ItemWriter<UpdatableRecord<?>> records = new DefaultLJooqItemWriter<>(context);
    ItemWriter<Collection<UpdatableRecord<?>>> batches =
        new CollectionItemWriter<>(records, BATCH_SIZE);
    return new ConvergingRelationItemWriter(dataSource, batches);
  }

  private LabelUrlRecord labelUrl(int labelId, String url) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return new LabelUrlRecord()
        .setLabelId(labelId)
        .setUrl(url)
        .setHash(url.hashCode())
        .setCreatedAt(now)
        .setLastModifiedAt(now);
  }
}
