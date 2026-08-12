package io.dsub.discogs.batch.dump.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.service.DefaultDiscogsDumpService;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiscogsDumpRepositoryIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final String TEST_SCHEMA = "dump_catalog_repository_test";
  private static final String CANONICAL_SCHEMA = "public";
  private static final String MIGRATION_RESOURCE =
      "/migrations/V002__discogs_dump_catalog.sql";
  private static final String CHECKSUM_A = "a".repeat(64);
  private static final String CHECKSUM_B = "b".repeat(64);

  private static JooqDiscogsDumpRepository repository;

  @BeforeAll
  static void createCatalogSchema() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("create schema " + TEST_SCHEMA);
      String migration;
      try (var input =
          DiscogsDumpRepositoryIntegrationTest.class.getResourceAsStream(MIGRATION_RESOURCE)) {
        if (input == null) {
          throw new IllegalStateException("missing migration " + MIGRATION_RESOURCE);
        }
        migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
      }
      statement.execute(migration.replace("public.", TEST_SCHEMA + "."));
    }
    Settings settings =
        new Settings()
            .withRenderMapping(
                new RenderMapping()
                    .withSchemata(
                        new MappedSchema()
                            .withInput(CANONICAL_SCHEMA)
                            .withOutput(TEST_SCHEMA)));
    DSLContext context = DSL.using(dataSource, SQLDialect.POSTGRES, settings);
    repository = new JooqDiscogsDumpRepository(context);
  }

  @AfterAll
  static void dropCatalogSchema() throws Exception {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("drop schema " + TEST_SCHEMA + " cascade");
    }
  }

  @BeforeEach
  void clearCatalog() {
    repository.deleteAll();
  }

  @Test
  void selectedRowsAreDurableIdempotentAndReconstructPinnedLocations() {
    LocalDate date = LocalDate.of(2026, 7, 1);
    DiscogsDump artist = dump(EntityType.ARTIST, date, CHECKSUM_A, 100L, "artist");
    DiscogsDump release = dump(EntityType.RELEASE, date, CHECKSUM_B, 200L, "release");

    repository.saveAll(List.of(artist, release));
    repository.saveAll(List.of(artist, release));

    assertThat(repository.count()).isEqualTo(2);
    assertThat(repository.findAll())
        .extracting(DiscogsDump::getType)
        .containsExactly(EntityType.ARTIST, EntityType.RELEASE);
    DiscogsDump stored = repository.findByETag(artist.getETag());
    assertThat(stored.getChecksumSha256()).isEqualTo(CHECKSUM_A);
    assertThat(stored.getChecksumUrl().toString())
        .isEqualTo(
            "https://data.discogs.com/"
                + "?download=data%2F2026%2Fdiscogs_20260701_CHECKSUM.txt");
    assertThat(stored.getUrl().toString())
        .isEqualTo(
            "https://data.discogs.com/?download=data%2F2026%2F"
                + "discogs_20260701_artists.xml.gz");
  }

  @Test
  void freshExactMonthResolutionPersistsThenReusesTheDurableCatalog() {
    YearMonth month = YearMonth.of(2026, 7);
    Set<EntityType> types = Set.of(EntityType.ARTIST);
    DiscogsDump selected = dump(EntityType.ARTIST, month.atDay(1), CHECKSUM_A, 1L, "fresh");
    DumpSupplier supplier = Mockito.mock(DumpSupplier.class);
    when(supplier.getMonth(types, month)).thenReturn(List.of(selected));
    DefaultDiscogsDumpService service = new DefaultDiscogsDumpService(repository, supplier);

    assertThat(service.resolveMonth(types, month)).containsExactly(selected);
    assertThat(repository.count()).isEqualTo(1);

    Mockito.reset(supplier);
    assertThat(service.resolveMonth(types, month)).containsExactly(selected);
    verify(supplier, never()).getMonth(types, month);
  }

  @Test
  void freshLatestResolutionPersistsThePinnedSelection() {
    Set<EntityType> types = Set.of(EntityType.RELEASE);
    DiscogsDump selected =
        dump(EntityType.RELEASE, LocalDate.of(2026, 7, 1), CHECKSUM_B, 1L, "latest");
    DumpSupplier supplier = Mockito.mock(DumpSupplier.class);
    when(supplier.getLatest(types)).thenReturn(List.of(selected));
    DefaultDiscogsDumpService service = new DefaultDiscogsDumpService(repository, supplier);

    assertThat(service.resolveLatest(types)).containsExactly(selected);
    assertThat(repository.findTopByType(EntityType.RELEASE).getChecksumSha256())
        .isEqualTo(CHECKSUM_B);
  }

  @Test
  void queryMethodsUseHalfOpenMonthRangesAndStableLatestOrdering() {
    LocalDate june = LocalDate.of(2026, 6, 1);
    LocalDate julyFirst = LocalDate.of(2026, 7, 1);
    LocalDate julyLater = LocalDate.of(2026, 7, 15);
    LocalDate august = LocalDate.of(2026, 8, 1);
    DiscogsDump juneArtist = dump(EntityType.ARTIST, june, CHECKSUM_A, 1L, "june");
    DiscogsDump julyArtist = dump(EntityType.ARTIST, julyFirst, CHECKSUM_A, 2L, "july-first");
    DiscogsDump laterArtist = dump(EntityType.ARTIST, julyLater, CHECKSUM_B, 3L, "july-later");
    DiscogsDump augustRelease = dump(EntityType.RELEASE, august, CHECKSUM_A, 4L, "august");
    repository.saveAll(List.of(juneArtist, julyArtist, laterArtist, augustRelease));

    assertThat(repository.findAllByLastModifiedAtIsBetween(julyFirst, august))
        .containsExactly(julyArtist, laterArtist);
    assertThat(
            repository.findByTypeAndLastModifiedAtBetween(
                EntityType.ARTIST, julyFirst, august))
        .containsExactly(julyArtist, laterArtist);
    assertThat(
            repository.findTopByTypeAndLastModifiedAtBetween(
                EntityType.ARTIST, julyFirst, august))
        .isEqualTo(laterArtist);
    assertThat(repository.findTopByType(EntityType.ARTIST)).isEqualTo(laterArtist);
    assertThat(repository.findTopByType(EntityType.LABEL)).isNull();
    assertThat(repository.countItemsAfter(julyFirst)).isEqualTo(3);
    assertThat(repository.countItemsBefore(julyFirst)).isEqualTo(2);
    assertThat(repository.countItemsBetween(julyFirst, august)).isEqualTo(2);
  }

  @Test
  void etagQueriesAndDeleteHandleAbsentValuesWithoutGuessing() {
    DiscogsDump dump =
        dump(EntityType.MASTER, LocalDate.of(2026, 7, 1), CHECKSUM_A, 1L, "master");
    repository.save(dump);

    assertThat(repository.existsByETag(dump.getETag())).isTrue();
    assertThat(repository.existsByETag("unknown")).isFalse();
    assertThat(repository.existsByETag(null)).isFalse();
    assertThat(repository.existsByETag(" ")).isFalse();
    assertThat(repository.findByETag(null)).isNull();
    assertThat(repository.findByETag(" ")).isNull();
    assertThat(repository.findByETag("unknown")).isNull();

    repository.deleteAll();
    assertThat(repository.findAll()).isEmpty();
  }

  @Test
  void saveNormalizesUnknownSizesAndAcceptsBothChecksumCases() {
    DiscogsDump nullSize =
        dump(EntityType.ARTIST, LocalDate.of(2026, 6, 1), CHECKSUM_A, null, "null-size");
    DiscogsDump negativeSize =
        dump(EntityType.LABEL, LocalDate.of(2026, 7, 1), CHECKSUM_B.toUpperCase(), -1L,
            "negative-size");
    repository.saveAll(List.of(nullSize, negativeSize));

    assertThat(repository.findByETag(nullSize.getETag()).getSize()).isZero();
    assertThat(repository.findByETag(negativeSize.getETag()).getSize()).isZero();
    assertThat(repository.findByETag(negativeSize.getETag()).getChecksumSha256())
        .isEqualTo(CHECKSUM_B);
  }

  @Test
  void reissuedStableIdentifierCannotOverwritePinnedChecksum() {
    LocalDate date = LocalDate.of(2026, 7, 1);
    DiscogsDump original = dump(EntityType.RELEASE, date, CHECKSUM_A, 1L, "same-path");
    DiscogsDump reissued = dump(EntityType.RELEASE, date, CHECKSUM_B, 1L, "same-path");

    repository.save(original);
    assertThatThrownBy(() -> repository.save(reissued))
        .isInstanceOf(org.jooq.exception.IntegrityConstraintViolationException.class)
        .hasMessageContaining("uq_discogs_dump_etag");

    assertThat(repository.count()).isEqualTo(1);
    assertThat(repository.findTopByType(EntityType.RELEASE).getChecksumSha256())
        .isEqualTo(CHECKSUM_A);
  }

  @Test
  void invalidSelectionsFailBeforeAnyTransactionWrites() {
    DiscogsDump valid =
        dump(EntityType.ARTIST, LocalDate.of(2026, 7, 1), CHECKSUM_A, 1L, "valid");
    assertThatThrownBy(() -> new JooqDiscogsDumpRepository(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> repository.saveAll(Arrays.asList(valid, null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dump cannot be null");
    assertThat(repository.count()).isZero();
    assertThatThrownBy(() -> repository.save(dumpWith(" ", "uri", CHECKSUM_A)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ETag");
    assertThatThrownBy(() -> repository.save(dumpWith(null, "uri", CHECKSUM_A)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ETag");
    assertThatThrownBy(() -> repository.save(dumpWith("etag", " ", CHECKSUM_A)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("URI");
    assertThatThrownBy(() -> repository.save(dumpWith("etag", null, CHECKSUM_A)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("URI");
    assertThatThrownBy(() -> repository.save(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dump cannot be null");
    assertThatThrownBy(
            () ->
                repository.save(
                    new DiscogsDump(
                        "etag", null, "uri", 1L, LocalDate.of(2026, 7, 1), null, null,
                        CHECKSUM_A)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("entity type");
    assertThatThrownBy(
            () ->
                repository.save(
                    new DiscogsDump(
                        "etag", EntityType.ARTIST, "uri", 1L, null, null, null, CHECKSUM_A)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("date");
    assertThatThrownBy(() -> repository.save(dumpWith("etag", "uri", null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SHA-256");
    assertThatThrownBy(() -> repository.save(dumpWith("etag", "uri", "a".repeat(63))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SHA-256");
    assertThatThrownBy(() -> repository.save(dumpWith("etag", "uri", "g".repeat(64))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SHA-256");
    assertThat(repository.count()).isZero();
  }

  @Test
  void emptySavesAreNoOps() {
    repository.saveAll(null);
    repository.saveAll(List.of());
    assertThat(repository.count()).isZero();
  }

  private static DiscogsDump dump(
      EntityType type,
      LocalDate date,
      String checksum,
      Long size,
      String identity) {
    String uri =
        "data/"
            + date.getYear()
            + "/discogs_"
            + date.toString().replace("-", "")
            + "_"
            + type
            + "s.xml.gz";
    return new DiscogsDump(identity, type, uri, size, date, null, null, checksum);
  }

  private static DiscogsDump dumpWith(String eTag, String uri, String checksum) {
    return new DiscogsDump(
        eTag,
        EntityType.ARTIST,
        uri,
        1L,
        LocalDate.of(2026, 7, 1),
        null,
        null,
        checksum);
  }
}
