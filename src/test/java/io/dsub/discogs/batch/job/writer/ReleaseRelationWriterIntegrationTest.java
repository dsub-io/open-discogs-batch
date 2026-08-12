package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.records.LabelReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemCreditedArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemFormatRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemIdentifierRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemStyleRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemTrackRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemVideoRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemWorkRecord;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.UpdatableRecord;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;

class ReleaseRelationWriterIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final String TEST_SCHEMA = "release_relation_writer_test";
  private static final String CARDINALITY_VIOLATION_SQL_STATE = "21000";
  private static final int RELEASE_ID = 2;
  private static final int ARTIST_ID = 5;
  private static final int LABEL_ID = 5;
  private static final LocalDateTime FIRST_WRITE = LocalDateTime.of(2026, 8, 1, 0, 0);
  private static final LocalDateTime SECOND_WRITE = LocalDateTime.of(2026, 8, 2, 0, 0);
  private static final LocalDateTime THIRD_WRITE = LocalDateTime.of(2026, 8, 3, 0, 0);

  private static HikariDataSource isolatedDataSource;
  private static JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void migrateIsolatedSchema() throws Exception {
    JdbcTemplate root = new JdbcTemplate(dataSource);
    root.execute("drop schema if exists " + TEST_SCHEMA + " cascade");
    root.execute("create schema " + TEST_SCHEMA);
    executeMigration(root, "migrations/V001__initial_schema.sql");
    executeMigration(root, "migrations/V008__label_release_catalog_identity.sql");

    String containerJdbcUrl = CONTAINER.getJdbcUrl();
    String delimiter = containerJdbcUrl.contains("?") ? "&" : "?";
    isolatedDataSource =
        DataSourceBuilder.<HikariDataSource>create()
            .type(HikariDataSource.class)
            .driverClassName(CONTAINER.getDriverClassName())
            .url(containerJdbcUrl + delimiter + "currentSchema=" + TEST_SCHEMA)
            .username(CONTAINER.getUsername())
            .password(CONTAINER.getPassword())
            .build();
    jdbcTemplate = new JdbcTemplate(isolatedDataSource);
  }

  @AfterAll
  static void dropIsolatedSchema() {
    if (isolatedDataSource != null) {
      isolatedDataSource.close();
    }
    new JdbcTemplate(dataSource).execute("drop schema if exists " + TEST_SCHEMA + " cascade");
  }

  @BeforeEach
  void resetRelations() {
    jdbcTemplate.execute(
        "truncate table release_item, artist, label, genre, style restart identity cascade");
    jdbcTemplate.update(
        "insert into artist (id, created_at, last_modified_at, name) values (?, now(), now(), ?)",
        ARTIST_ID,
        "Artist");
    jdbcTemplate.update(
        "insert into label (id, created_at, last_modified_at, name) values (?, now(), now(), ?)",
        LABEL_ID,
        "Label");
    jdbcTemplate.update("insert into genre (name) values (?)", "Rock");
    jdbcTemplate.update("insert into style (name) values (?)", "House");
    jdbcTemplate.update(
        "insert into release_item (id, created_at, last_modified_at, title) values (?, now(), now(), ?)",
        RELEASE_ID,
        "Release");
  }

  @Test
  void canonicalBatchAvoidsCardinalityViolationAndRerunsIdempotently() throws Exception {
    assertThatThrownBy(this::executeKnownCardinalityViolation)
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .isInstanceOf(SQLException.class)
        .extracting(cause -> ((SQLException) cause).getSQLState())
        .isEqualTo(CARDINALITY_VIOLATION_SQL_STATE);

    ItemWriter<RelationSet> writer = writer();
    writer.write(new Chunk<>(List.of(relationSet(1, FIRST_WRITE))));
    int formatId = jdbcTemplate.queryForObject("select id from release_item_format", Integer.class);
    LocalDateTime firstModified =
        jdbcTemplate.queryForObject(
            "select last_modified_at from release_item_format", LocalDateTime.class);

    writer.write(new Chunk<>(List.of(relationSet(1, SECOND_WRITE))));

    assertRelationCounts();
    assertThat(jdbcTemplate.queryForObject("select id from release_item_format", Integer.class))
        .isEqualTo(formatId);
    assertThat(
            jdbcTemplate.queryForObject(
                "select last_modified_at from release_item_format", LocalDateTime.class))
        .isEqualTo(firstModified);

    writer.write(new Chunk<>(List.of(relationSet(2, THIRD_WRITE))));

    assertRelationCounts();
    assertThat(jdbcTemplate.queryForObject("select id from release_item_format", Integer.class))
        .isEqualTo(formatId);
    assertThat(
            jdbcTemplate.queryForObject(
                "select quantity from release_item_format", Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbcTemplate.queryForObject(
                "select last_modified_at from release_item_format", LocalDateTime.class))
        .isEqualTo(THIRD_WRITE);
    assertThat(
            jdbcTemplate.queryForList(
                "select category_notation from label_release_item order by id", String.class))
        .containsExactly(null, "SK 026", "SK026");
  }

  private static void executeMigration(JdbcTemplate template, String resource) throws Exception {
    String source =
        new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
    String scoped = source.replace("public.", TEST_SCHEMA + ".");
    template.execute(
        (ConnectionCallback<Void>) connection -> {
          try (Statement statement = connection.createStatement()) {
            statement.execute(scoped);
          }
          return null;
        });
  }

  private void executeKnownCardinalityViolation() {
    jdbcTemplate.execute(
        """
        insert into release_item_artist
            (created_at, last_modified_at, artist_id, release_item_id)
        values
            (now(), now(), 5, 2),
            (now(), now(), 5, 2)
        on conflict (release_item_id, artist_id)
        do update set last_modified_at = excluded.last_modified_at
        """);
  }

  private ItemWriter<RelationSet> writer() {
    Settings settings =
        new Settings()
            .withRenderMapping(
                new RenderMapping()
                    .withSchemata(
                        new MappedSchema().withInput("public").withOutput(TEST_SCHEMA)));
    DSLContext context = DSL.using(isolatedDataSource, SQLDialect.POSTGRES, settings);
    ItemWriter<UpdatableRecord<?>> records = new DefaultLJooqItemWriter<>(context);
    ItemWriter<Collection<UpdatableRecord<?>>> batches =
        new CollectionItemWriter<>(records, 100);
    return new ConvergingRelationItemWriter(isolatedDataSource, batches);
  }

  private RelationSet relationSet(int quantity, LocalDateTime modifiedAt) {
    List<UpdatableRecord<?>> records = new ArrayList<>();
    records.add(artist(modifiedAt));
    records.add(label(null, modifiedAt));
    records.add(label("SK 026", modifiedAt));
    records.add(label("SK026", modifiedAt));
    records.add(genre(modifiedAt));
    records.add(style(modifiedAt));
    records.add(format(quantity, modifiedAt));
    records.add(track(modifiedAt));
    records.add(identifier(modifiedAt));
    records.add(work(modifiedAt));
    records.add(video(modifiedAt));
    records.add(creditedArtist(modifiedAt));
    records.addAll(List.copyOf(records));
    return new RelationSet(EntityType.RELEASE, RELEASE_ID, records);
  }

  private void assertRelationCounts() {
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_artist", Long.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from label_release_item", Long.class))
        .isEqualTo(3);
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_genre", Long.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_style", Long.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_format", Long.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_track", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from release_item_identifier", Long.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_work", Long.class))
        .isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_video", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from release_item_credited_artist", Long.class))
        .isEqualTo(1);
  }

  private ReleaseItemArtistRecord artist(LocalDateTime modifiedAt) {
    return new ReleaseItemArtistRecord()
        .setReleaseItemId(RELEASE_ID)
        .setArtistId(ARTIST_ID)
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private LabelReleaseItemRecord label(String categoryNotation, LocalDateTime modifiedAt) {
    return new LabelReleaseItemRecord()
        .setReleaseItemId(RELEASE_ID)
        .setLabelId(LABEL_ID)
        .setCategoryNotation(categoryNotation)
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemGenreRecord genre(LocalDateTime modifiedAt) {
    return new ReleaseItemGenreRecord()
        .setReleaseItemId(RELEASE_ID)
        .setGenre("Rock")
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemStyleRecord style(LocalDateTime modifiedAt) {
    return new ReleaseItemStyleRecord()
        .setReleaseItemId(RELEASE_ID)
        .setStyle("House")
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemFormatRecord format(int quantity, LocalDateTime modifiedAt) {
    return new ReleaseItemFormatRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(101)
        .setName("Vinyl")
        .setDescription("[d:LP]")
        .setText("Limited")
        .setQuantity(quantity)
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemTrackRecord track(LocalDateTime modifiedAt) {
    return new ReleaseItemTrackRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(102)
        .setPosition("A1")
        .setTitle("Track")
        .setDuration("3:00")
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemIdentifierRecord identifier(LocalDateTime modifiedAt) {
    return new ReleaseItemIdentifierRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(103)
        .setType("Barcode")
        .setDescription("Text")
        .setValue("123")
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemWorkRecord work(LocalDateTime modifiedAt) {
    return new ReleaseItemWorkRecord()
        .setReleaseItemId(RELEASE_ID)
        .setLabelId(LABEL_ID)
        .setHash(104)
        .setWork("Pressed By")
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemVideoRecord video(LocalDateTime modifiedAt) {
    return new ReleaseItemVideoRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(105)
        .setTitle("Video")
        .setDescription("Description")
        .setUrl("https://video.example")
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemCreditedArtistRecord creditedArtist(LocalDateTime modifiedAt) {
    return new ReleaseItemCreditedArtistRecord()
        .setReleaseItemId(RELEASE_ID)
        .setArtistId(ARTIST_ID)
        .setHash(106)
        .setRole("Producer")
        .setCreatedAt(FIRST_WRITE)
        .setLastModifiedAt(modifiedAt);
  }
}
