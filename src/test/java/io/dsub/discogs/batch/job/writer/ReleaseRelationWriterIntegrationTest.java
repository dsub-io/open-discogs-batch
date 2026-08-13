package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.domain.release.ReleaseRelationIdentity;
import io.dsub.discogs.batch.job.processor.RelationSet;
import io.dsub.opendiscogs.jooq.tables.records.LabelReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemCreditedArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemFormatRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemGenreRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemIdentifierRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemImageRecord;
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
  private static final int KNOWN_COLLISION_RELEASE_ID = 4_846_884;
  private static final int KNOWN_COLLISION_HASH = 86_171;
  private static final int OVERSIZED_QUANTITY_RELEASE_ID = 6_662_697;
  private static final String OVERSIZED_QUANTITY =
      "1010487400000000000000000000000000000000000000000000";
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
    for (String resource : List.of(
        "migrations/V001__initial_schema.sql",
        "migrations/V002__discogs_dump_catalog.sql",
        "migrations/V003__discogs_import_history.sql",
        "migrations/V004__allow_reissued_dump_paths.sql",
        "migrations/V005__durable_import_progress.sql",
        "migrations/V006__concurrent_import_progress.sql",
        "migrations/V007__api_query_indexes.sql",
        "migrations/V008__label_release_catalog_identity.sql",
        "migrations/V009__release_convergence_contract.sql",
        "migrations/V010__release_credited_artist_identity.sql",
        "migrations/V011__release_format_identity.sql",
        "migrations/V012__release_identifier_identity.sql",
        "migrations/V013__release_image_identity.sql",
        "migrations/V014__release_track_identity.sql",
        "migrations/V015__release_video_identity.sql",
        "migrations/V016__release_work_identity.sql",
        "migrations/V017__artist_alias_ordinal.sql",
        "migrations/V018__artist_group_ordinal.sql",
        "migrations/V019__artist_member_ordinal.sql",
        "migrations/V020__artist_name_variation_ordinal.sql",
        "migrations/V021__artist_url_ordinal.sql",
        "migrations/V022__label_sub_label_ordinal.sql",
        "migrations/V023__label_url_ordinal.sql",
        "migrations/V024__master_artist_ordinal.sql",
        "migrations/V025__master_genre_ordinal.sql",
        "migrations/V026__master_style_ordinal.sql",
        "migrations/V027__master_video_ordinal.sql",
        "migrations/V028__label_release_item_ordinal.sql",
        "migrations/V029__release_item_artist_ordinal.sql",
        "migrations/V030__release_item_credited_artist_ordinal.sql",
        "migrations/V031__release_item_format_ordinal.sql",
        "migrations/V032__release_item_genre_ordinal.sql",
        "migrations/V033__release_item_identifier_ordinal.sql",
        "migrations/V034__release_item_image_ordinal.sql",
        "migrations/V035__release_item_style_ordinal.sql",
        "migrations/V036__release_item_track_ordinal.sql",
        "migrations/V037__release_item_video_ordinal.sql",
        "migrations/V038__release_item_work_ordinal.sql",
        "migrations/V039__remove_relation_created_at.sql")) {
      executeMigration(root, resource);
    }

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
    jdbcTemplate.update(
        "insert into release_item (id, created_at, last_modified_at, title) values (?, now(), now(), ?)",
        KNOWN_COLLISION_RELEASE_ID,
        "Known collision release");
    jdbcTemplate.update(
        "insert into release_item (id, created_at, last_modified_at, title) values (?, now(), now(), ?)",
        OVERSIZED_QUANTITY_RELEASE_ID,
        "Known oversized quantity release");
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
        .isNotEqualTo(formatId);
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

  @Test
  void knownDiscogsTrackHashCollisionPersistsBothRowsAndRetriesIdempotently() throws Exception {
    ItemWriter<RelationSet> writer = writer();
    List<ReleaseItemTrackRecord> tracks = knownCollisionTracks();

    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    KNOWN_COLLISION_RELEASE_ID,
                    List.copyOf(tracks)))));

    List<Integer> firstIds =
        jdbcTemplate.queryForList(
            "select id from release_item_track where release_item_id = ? order by id",
            Integer.class,
            KNOWN_COLLISION_RELEASE_ID);
    assertThat(firstIds).hasSize(2);
    assertThat(
            jdbcTemplate.queryForList(
                "select hash from release_item_track where release_item_id = ? order by hash",
                Integer.class,
                KNOWN_COLLISION_RELEASE_ID))
        .containsExactly(-947_370_883, KNOWN_COLLISION_HASH);

    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    KNOWN_COLLISION_RELEASE_ID,
                    List.copyOf(knownCollisionTracks())))));

    assertThat(
            jdbcTemplate.queryForList(
                "select id from release_item_track where release_item_id = ? order by id",
                Integer.class,
                KNOWN_COLLISION_RELEASE_ID))
        .containsExactlyElementsOf(firstIds);
  }

  @Test
  void legacyNullDigestIsReplacedOnceAndThenRetainsItsPhysicalId() throws Exception {
    int legacyId =
        jdbcTemplate.queryForObject(
            """
            insert into release_item_track
                (last_modified_at, release_item_id, hash, position, title, duration)
            values (?, ?, ?, ?, ?, ?)
            returning id
            """,
            Integer.class,
            FIRST_WRITE,
            KNOWN_COLLISION_RELEASE_ID,
            KNOWN_COLLISION_HASH,
            "6",
            "Яд",
            null);
    ItemWriter<RelationSet> writer = writer();

    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    KNOWN_COLLISION_RELEASE_ID,
                    List.copyOf(knownCollisionTracks())))));

    List<Integer> backfilledIds =
        jdbcTemplate.queryForList(
            "select id from release_item_track where release_item_id = ? order by id",
            Integer.class,
            KNOWN_COLLISION_RELEASE_ID);
    assertThat(backfilledIds).hasSize(2).doesNotContain(legacyId);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from release_item_track where release_item_id = ? and octet_length(identity_sha256) = 32",
                Integer.class,
                KNOWN_COLLISION_RELEASE_ID))
        .isEqualTo(2);

    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    KNOWN_COLLISION_RELEASE_ID,
                    List.copyOf(knownCollisionTracks())))));

    assertThat(
            jdbcTemplate.queryForList(
                "select id from release_item_track where release_item_id = ? order by id",
                Integer.class,
                KNOWN_COLLISION_RELEASE_ID))
        .containsExactlyElementsOf(backfilledIds);
  }

  @Test
  void remainingCollisionRowMovesBackToItsLegacySlotAndThenStabilizes() throws Exception {
    ItemWriter<RelationSet> writer = writer();
    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    KNOWN_COLLISION_RELEASE_ID,
                    List.copyOf(knownCollisionTracks())))));
    int collidedId =
        jdbcTemplate.queryForObject(
            "select id from release_item_track where release_item_id = ? and title = ?",
            Integer.class,
            KNOWN_COLLISION_RELEASE_ID,
            "Ад");

    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    KNOWN_COLLISION_RELEASE_ID,
                    List.of(knownCollisionTracks().get(1))))));

    int reassignedId =
        jdbcTemplate.queryForObject(
            "select id from release_item_track where release_item_id = ?",
            Integer.class,
            KNOWN_COLLISION_RELEASE_ID);
    assertThat(reassignedId).isNotEqualTo(collidedId);
    assertThat(
            jdbcTemplate.queryForObject(
                "select hash from release_item_track where id = ?", Integer.class, reassignedId))
        .isEqualTo(KNOWN_COLLISION_HASH);

    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    KNOWN_COLLISION_RELEASE_ID,
                    List.of(knownCollisionTracks().get(1))))));
    assertThat(
            jdbcTemplate.queryForObject(
                "select id from release_item_track where release_item_id = ?",
                Integer.class,
                KNOWN_COLLISION_RELEASE_ID))
        .isEqualTo(reassignedId);
  }

  @Test
  void oversizedFormatQuantityPersistsAndRetriesIdempotently() throws Exception {
    ItemWriter<RelationSet> writer = writer();
    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    OVERSIZED_QUANTITY_RELEASE_ID,
                    List.of(oversizedQuantityFormat())))));

    int firstId =
        jdbcTemplate.queryForObject(
            "select id from release_item_format where release_item_id = ?",
            Integer.class,
            OVERSIZED_QUANTITY_RELEASE_ID);
    assertThat(
            jdbcTemplate.queryForObject(
                "select quantity_text from release_item_format where id = ?",
                String.class,
                firstId))
        .isEqualTo(OVERSIZED_QUANTITY);
    assertThat(
            jdbcTemplate.queryForObject(
                "select quantity is null from release_item_format where id = ?",
                Boolean.class,
                firstId))
        .isTrue();

    writer.write(
        new Chunk<>(
            List.of(
                new RelationSet(
                    EntityType.RELEASE,
                    OVERSIZED_QUANTITY_RELEASE_ID,
                    List.of(oversizedQuantityFormat())))));
    assertThat(
            jdbcTemplate.queryForObject(
                "select id from release_item_format where release_item_id = ?",
                Integer.class,
                OVERSIZED_QUANTITY_RELEASE_ID))
        .isEqualTo(firstId);
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
            (last_modified_at, artist_id, release_item_id)
        values
            (now(), 5, 2),
            (now(), 5, 2)
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
    records.add(image(modifiedAt));
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
    assertThat(jdbcTemplate.queryForObject("select count(*) from release_item_image", Long.class))
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
        .setLastModifiedAt(modifiedAt);
  }

  private LabelReleaseItemRecord label(String categoryNotation, LocalDateTime modifiedAt) {
    return new LabelReleaseItemRecord()
        .setReleaseItemId(RELEASE_ID)
        .setLabelId(LABEL_ID)
        .setCategoryNotation(categoryNotation)
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemGenreRecord genre(LocalDateTime modifiedAt) {
    return new ReleaseItemGenreRecord()
        .setReleaseItemId(RELEASE_ID)
        .setGenre("Rock")
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemStyleRecord style(LocalDateTime modifiedAt) {
    return new ReleaseItemStyleRecord()
        .setReleaseItemId(RELEASE_ID)
        .setStyle("House")
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
        .setQuantityText(Integer.toString(quantity))
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.FORMAT,
                "Vinyl", "[d:LP]", Integer.toString(quantity), "Limited"))
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemTrackRecord track(LocalDateTime modifiedAt) {
    return new ReleaseItemTrackRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(102)
        .setPosition("A1")
        .setTitle("Track")
        .setDuration("3:00")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, "A1", "Track", "3:00"))
        .setLastModifiedAt(modifiedAt);
  }

  private List<ReleaseItemTrackRecord> knownCollisionTracks() {
    return List.of(
        collisionTrack("6", "Яд"),
        collisionTrack("7", "Ад"));
  }

  private ReleaseItemTrackRecord collisionTrack(String position, String title) {
    return new ReleaseItemTrackRecord()
        .setReleaseItemId(KNOWN_COLLISION_RELEASE_ID)
        .setHash(KNOWN_COLLISION_HASH)
        .setPosition(position)
        .setTitle(title)
        .setDuration(null)
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.TRACK, position, title, null))
        .setLastModifiedAt(FIRST_WRITE);
  }

  private ReleaseItemFormatRecord oversizedQuantityFormat() {
    return new ReleaseItemFormatRecord()
        .setReleaseItemId(OVERSIZED_QUANTITY_RELEASE_ID)
        .setHash(700)
        .setName("File")
        .setQuantity(null)
        .setQuantityText(OVERSIZED_QUANTITY)
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.FORMAT,
                "File", null, OVERSIZED_QUANTITY, null))
        .setLastModifiedAt(FIRST_WRITE);
  }

  private ReleaseItemIdentifierRecord identifier(LocalDateTime modifiedAt) {
    return new ReleaseItemIdentifierRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(103)
        .setType("Barcode")
        .setDescription("Text")
        .setValue("123")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.IDENTIFIER, "Barcode", "Text", "123"))
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemImageRecord image(LocalDateTime modifiedAt) {
    return new ReleaseItemImageRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(107)
        .setFileName("cover.jpg")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.IMAGE, "cover.jpg"))
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemWorkRecord work(LocalDateTime modifiedAt) {
    return new ReleaseItemWorkRecord()
        .setReleaseItemId(RELEASE_ID)
        .setLabelId(LABEL_ID)
        .setHash(104)
        .setWork("Pressed By")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.WORK, "Pressed By"))
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemVideoRecord video(LocalDateTime modifiedAt) {
    return new ReleaseItemVideoRecord()
        .setReleaseItemId(RELEASE_ID)
        .setHash(105)
        .setTitle("Video")
        .setDescription("Description")
        .setUrl("https://video.example")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.VIDEO,
                "Video", "Description", "https://video.example"))
        .setLastModifiedAt(modifiedAt);
  }

  private ReleaseItemCreditedArtistRecord creditedArtist(LocalDateTime modifiedAt) {
    return new ReleaseItemCreditedArtistRecord()
        .setReleaseItemId(RELEASE_ID)
        .setArtistId(ARTIST_ID)
        .setHash(106)
        .setRole("Producer")
        .setIdentitySha256(
            ReleaseRelationIdentity.digest(
                ReleaseRelationIdentity.Relation.CREDITED_ARTIST, "Producer"))
        .setLastModifiedAt(modifiedAt);
  }
}
