package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.container.PostgreSQLIntegrationSupport;
import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CanonicalSchemaMigratorIntegrationTest extends PostgreSQLIntegrationSupport {

  private static final String PUBLIC_CHANGELOG =
      "db/changelog/db.changelog-master.xml";
  private static final String CUSTOM_CHANGELOG =
      "db/changelog/db.changelog-custom-schema.xml";
  private static final String EXECUTED = "EXECUTED";
  private static final String MARK_RAN = "MARK_RAN";
  private static final String CANONICAL_LEDGER_TABLE = "open_discogs_schema_migration";
  private static final AtomicInteger SCHEMA_SEQUENCE = new AtomicInteger();

  private final CanonicalMigrationSource migrationSource = new CanonicalMigrationSource();
  private final List<CanonicalMigration> migrations = migrationSource.load();
  private final LegacyLiquibaseContract contract =
      new LegacyLiquibaseContractSource().load(migrations);
  private DatabaseSchema testSchema;

  @BeforeEach
  void createTestSchema() throws SQLException {
    testSchema =
        new DatabaseSchema("legacy_adoption_" + SCHEMA_SEQUENCE.incrementAndGet());
    execute("create schema " + testSchema.quotedName());
  }

  @AfterEach
  void dropTestSchema() throws SQLException {
    execute("drop schema if exists " + testSchema.quotedName() + " cascade");
  }

  @ParameterizedTest
  @ValueSource(strings = {"V004", "V006", "V007"})
  void adoptsEverySupportedExactPublicPrefix(String prefix) throws SQLException {
    installLegacySchema(prefix);
    createLegacyTables(false);
    insertHistory(history(prefix, LegacySchemaMode.PUBLIC));

    migrate();

    assertCanonicalLedgerComplete();
    assertReleaseContractRevisionColumn();
  }

  @Test
  void adoptsCustomHistoryOnlyAfterMatchingSchemaFingerprint() throws SQLException {
    installLegacySchema("V007");
    createLegacyTables(false);
    insertHistory(history("V007", LegacySchemaMode.CUSTOM));

    migrate();

    assertCanonicalLedgerComplete();
  }

  @Test
  void adoptsMarkRanHistoryOnlyAfterMatchingSchemaFingerprint() throws SQLException {
    installLegacySchema("V006");
    createLegacyTables(false);
    List<LegacyHistorySpec> history = mutableHistory("V006", LegacySchemaMode.PUBLIC);
    history.set(0, history.getFirst().withExecutionType(MARK_RAN));
    insertHistory(history);

    migrate();

    assertCanonicalLedgerComplete();
  }

  @Test
  void extendsAnExistingCanonicalPrefixInTheSameAdoption() throws SQLException {
    installLegacySchema("V007");
    createCanonicalLedger();
    insertCanonicalPrefix(2);
    createLegacyTables(false);
    insertHistory(history("V007", LegacySchemaMode.CUSTOM));

    migrate();

    assertCanonicalLedgerComplete();
  }

  @Test
  void validatesLegacyHistoryButDoesNotReadoptAnAlreadyCompleteCanonicalLedger()
      throws SQLException {
    installCanonicalSchema(migrations.size());
    createCanonicalLedger();
    insertCanonicalPrefix(migrations.size());
    createLegacyTables(false);
    insertHistory(history("V007", LegacySchemaMode.CUSTOM));

    migrate();

    assertCanonicalLedgerComplete();
  }

  @ParameterizedTest(name = "rejects untrusted history: {0}")
  @MethodSource("untrustedHistories")
  void rejectsMissingDuplicateUnknownAndMismatchedHistoryAtomically(
      String scenario, Consumer<List<LegacyHistorySpec>> mutation) throws SQLException {
    installLegacySchema("V007");
    createLegacyTables(false);
    List<LegacyHistorySpec> history = mutableHistory("V007", LegacySchemaMode.PUBLIC);
    mutation.accept(history);
    insertHistory(history);

    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class);
    assertThat(canonicalLedgerVersions()).isEmpty();
  }

  @Test
  void rejectsAChangedCustomSchemaFingerprintWithoutAdoptingRows() throws SQLException {
    installLegacySchema("V007");
    createLegacyTables(false);
    insertHistory(history("V007", LegacySchemaMode.CUSTOM));
    execute(
        "alter table "
            + qualified("artist")
            + " add column unexpected_legacy_column integer");

    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("legacy schema fingerprint mismatch");
    assertThat(canonicalLedgerVersions()).isEmpty();
  }

  @Test
  void rejectsMissingOrActiveLiquibaseLockState() throws SQLException {
    installLegacySchema("V004");
    createLegacyTables(true);
    insertHistory(history("V004", LegacySchemaMode.PUBLIC));

    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("without its lock table");
    assertThat(canonicalLedgerVersions()).isEmpty();

    execute(
        "create table "
            + qualified("databasechangeloglock")
            + " (id integer not null, locked boolean not null)");
    execute(
        "insert into " + qualified("databasechangeloglock") + " values (1, true)");

    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("legacy Liquibase migration is active");
    assertThat(canonicalLedgerVersions()).isEmpty();
  }

  @Test
  void rejectsMissingAndUnexpectedLiquibaseLockRows() throws SQLException {
    installLegacySchema("V004");
    createLegacyTables(false);
    insertHistory(history("V004", LegacySchemaMode.PUBLIC));
    execute("delete from " + qualified("databasechangeloglock"));

    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("exactly lock id 1");

    execute("insert into " + qualified("databasechangeloglock") + " values (2, false)");
    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("exactly lock id 1");
    assertThat(canonicalLedgerVersions()).isEmpty();
  }

  @Test
  void rollsBackEveryAdoptedRowWhenOneLedgerInsertFails() throws SQLException {
    installLegacySchema("V007");
    createCanonicalLedger();
    createRejectingLedgerTrigger();
    createLegacyTables(false);
    insertHistory(history("V007", LegacySchemaMode.PUBLIC));

    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("reject V004 for atomicity test");
    assertThat(canonicalLedgerVersions()).isEmpty();
  }

  @Test
  void ignoresUnrelatedLiquibaseRowsAndRunsCanonicalMigrationsNormally() throws SQLException {
    createLegacyTables(false);
    insertHistory(
        List.of(
            new LegacyHistorySpec(
                "unrelated-change", "someone", "other.xml", EXECUTED, null, 1)));

    migrate();

    assertCanonicalLedgerComplete();
  }

  @ParameterizedTest(name = "rejects non-canonical shared ledger: {0}")
  @MethodSource("invalidCanonicalLedgers")
  void rejectsNewerGappedAndChecksumMismatchedCanonicalLedgers(
      String scenario, Consumer<List<CanonicalLedgerSpec>> mutation) throws SQLException {
    createCanonicalLedger();
    List<CanonicalLedgerSpec> rows = new ArrayList<>();
    for (CanonicalMigration migration : migrations) {
      rows.add(new CanonicalLedgerSpec(migration.version(), migration.checksum()));
    }
    mutation.accept(rows);
    insertCanonicalRows(rows);

    assertThatThrownBy(this::migrate)
        .isInstanceOf(InitializationFailureException.class);
  }

  private static Stream<Arguments> untrustedHistories() {
    return Stream.of(
        Arguments.of(
            "missing",
            (Consumer<List<LegacyHistorySpec>>) history -> history.remove(2)),
        Arguments.of(
            "unsupported-row-count",
            (Consumer<List<LegacyHistorySpec>>)
                history -> {
                  history.remove(5);
                  history.remove(2);
                }),
        Arguments.of(
            "duplicate",
            (Consumer<List<LegacyHistorySpec>>) history -> history.add(history.getFirst())),
        Arguments.of(
            "unknown-id",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(6, history.get(6).withId("open-discogs-model-v999"))),
        Arguments.of(
            "wrong-author",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(3, history.get(3).withAuthor("unknown"))),
        Arguments.of(
            "mixed-filename",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(4, history.get(4).withFilename(CUSTOM_CHANGELOG))),
        Arguments.of(
            "unknown-first-filename",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(0, history.getFirst().withFilename("unknown.xml"))),
        Arguments.of(
            "unknown-execution-type",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(2, history.get(2).withExecutionType("RERAN"))),
        Arguments.of(
            "forbidden-mark-ran",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(3, history.get(3).withExecutionType(MARK_RAN))),
        Arguments.of(
            "checksum-mismatch",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(1, history.get(1).withChecksum("9:" + "0".repeat(32)))),
        Arguments.of(
            "non-increasing-order",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(1, history.get(1).withOrderExecuted(1))),
        Arguments.of(
            "null-author",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(1, history.get(1).withAuthor(null))),
        Arguments.of(
            "blank-author",
            (Consumer<List<LegacyHistorySpec>>)
                history -> history.set(1, history.get(1).withAuthor(" "))));
  }

  private static Stream<Arguments> invalidCanonicalLedgers() {
    return Stream.of(
        Arguments.of(
            "newer",
            (Consumer<List<CanonicalLedgerSpec>>)
                rows -> rows.add(new CanonicalLedgerSpec("V999__future.sql", "0".repeat(64)))),
        Arguments.of(
            "gap",
            (Consumer<List<CanonicalLedgerSpec>>)
                rows -> rows.set(1, new CanonicalLedgerSpec("V999__gap.sql", "0".repeat(64)))),
        Arguments.of(
            "checksum",
            (Consumer<List<CanonicalLedgerSpec>>)
                rows -> rows.set(0, new CanonicalLedgerSpec(rows.getFirst().version(), "0".repeat(64)))));
  }

  private void migrate() {
    new CanonicalSchemaMigrator(dataSource, testSchema).migrate();
  }

  private void installLegacySchema(String prefix) throws SQLException {
    int count = contract.schemaContractForPrefix(prefix).migrationVersions().size();
    installCanonicalSchema(count);
  }

  private void installCanonicalSchema(int count) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "set search_path to " + testSchema.quotedName() + ", \"public\"");
      for (int index = 0; index < count; index++) {
        statement.execute(scopedSql(migrations.get(index)));
      }
    }
  }

  private String scopedSql(CanonicalMigration migration) {
    return migration.sql().replace("public.", testSchema.quotedName() + '.');
  }

  private void createLegacyTables(boolean omitLockTable) throws SQLException {
    execute(
        "create table "
            + qualified("databasechangelog")
            + " (id varchar(255), author varchar(255), filename varchar(255), exectype varchar(20), md5sum varchar(35), orderexecuted integer)");
    if (!omitLockTable) {
      execute(
          "create table "
              + qualified("databasechangeloglock")
              + " (id integer not null, locked boolean not null)");
      execute(
          "insert into " + qualified("databasechangeloglock") + " values (1, false)");
    }
  }

  private List<LegacyHistorySpec> history(String prefix, LegacySchemaMode mode) {
    return List.copyOf(mutableHistory(prefix, mode));
  }

  private List<LegacyHistorySpec> mutableHistory(String prefix, LegacySchemaMode mode) {
    List<LegacyHistorySpec> history = new ArrayList<>();
    List<String> versions = contract.schemaContractForPrefix(prefix).migrationVersions();
    for (int index = 0; index < versions.size(); index++) {
      LegacyMigrationContract migration = contract.migrations().get(index);
      LegacyChangeSetContract changeSet = migration.changeSetFor(mode);
      String checksum =
          changeSet.checksumRule() instanceof ExactLegacyChecksum exact
              ? exact.checksum()
              : "9:" + "%032x".formatted(index + 1);
      history.add(
          new LegacyHistorySpec(
              changeSet.id(),
              changeSet.author(),
              changeSet.filename(),
              EXECUTED,
              checksum,
              index + 1));
    }
    return history;
  }

  private void insertHistory(List<LegacyHistorySpec> history) throws SQLException {
    String sql =
        "insert into "
            + qualified("databasechangelog")
            + " (id, author, filename, exectype, md5sum, orderexecuted) values (?, ?, ?, ?, ?, ?)";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (LegacyHistorySpec row : history) {
        statement.setString(1, row.id());
        statement.setString(2, row.author());
        statement.setString(3, row.filename());
        statement.setString(4, row.executionType());
        statement.setString(5, row.checksum());
        statement.setInt(6, row.orderExecuted());
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  private void createCanonicalLedger() throws SQLException {
    execute(
        "create table "
            + qualified(CANONICAL_LEDGER_TABLE)
            + " (version varchar(255) primary key, checksum char(64) not null, applied_at timestamp not null default now())");
  }

  private void insertCanonicalPrefix(int count) throws SQLException {
    List<CanonicalLedgerSpec> rows = new ArrayList<>();
    for (int index = 0; index < count; index++) {
      rows.add(
          new CanonicalLedgerSpec(
              migrations.get(index).version(), migrations.get(index).checksum()));
    }
    insertCanonicalRows(rows);
  }

  private void insertCanonicalRows(List<CanonicalLedgerSpec> rows) throws SQLException {
    String sql =
        "insert into "
            + qualified(CANONICAL_LEDGER_TABLE)
            + " (version, checksum) values (?, ?)";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (CanonicalLedgerSpec row : rows) {
        statement.setString(1, row.version());
        statement.setString(2, row.checksum());
        statement.addBatch();
      }
      statement.executeBatch();
    }
  }

  private void createRejectingLedgerTrigger() throws SQLException {
    execute(
        "create function "
            + testSchema.quotedName()
            + ".reject_v004() returns trigger language plpgsql as $body$ begin if new.version like 'V004\\_\\_%' escape '\\' then raise exception 'reject V004 for atomicity test'; end if; return new; end $body$");
    execute(
        "create trigger reject_v004 before insert on "
            + qualified(CANONICAL_LEDGER_TABLE)
            + " for each row execute function "
            + testSchema.quotedName()
            + ".reject_v004()");
  }

  private List<String> canonicalLedgerVersions() throws SQLException {
    if (!tableExists(CANONICAL_LEDGER_TABLE)) {
      return List.of();
    }
    List<String> versions = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet result =
            statement.executeQuery(
                "select version from "
                    + qualified(CANONICAL_LEDGER_TABLE)
                    + " order by version")) {
      while (result.next()) {
        versions.add(result.getString(1));
      }
    }
    return List.copyOf(versions);
  }

  private void assertCanonicalLedgerComplete() throws SQLException {
    assertThat(canonicalLedgerVersions())
        .containsExactlyElementsOf(migrations.stream().map(CanonicalMigration::version).toList());
  }

  private void assertReleaseContractRevisionColumn() throws SQLException {
    String sql =
        "select exists(select 1 from information_schema.columns where table_schema = ? and table_name = 'discogs_import_run_dump' and column_name = 'import_contract_revision')";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, testSchema.name());
      try (ResultSet result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        assertThat(result.getBoolean(1)).isTrue();
      }
    }
  }

  private boolean tableExists(String table) throws SQLException {
    String sql =
        "select exists(select 1 from information_schema.tables where table_schema = ? and table_name = ?)";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, testSchema.name());
      statement.setString(2, table);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() && result.getBoolean(1);
      }
    }
  }

  private void execute(String sql) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private String qualified(String table) {
    return testSchema.quotedName() + ".\"" + table + '"';
  }

  private record LegacyHistorySpec(
      String id,
      String author,
      String filename,
      String executionType,
      String checksum,
      int orderExecuted) {

    LegacyHistorySpec withId(String value) {
      return new LegacyHistorySpec(
          value, author, filename, executionType, checksum, orderExecuted);
    }

    LegacyHistorySpec withAuthor(String value) {
      return new LegacyHistorySpec(
          id, value, filename, executionType, checksum, orderExecuted);
    }

    LegacyHistorySpec withFilename(String value) {
      return new LegacyHistorySpec(
          id, author, value, executionType, checksum, orderExecuted);
    }

    LegacyHistorySpec withExecutionType(String value) {
      return new LegacyHistorySpec(id, author, filename, value, checksum, orderExecuted);
    }

    LegacyHistorySpec withChecksum(String value) {
      return new LegacyHistorySpec(
          id, author, filename, executionType, value, orderExecuted);
    }

    LegacyHistorySpec withOrderExecuted(int value) {
      return new LegacyHistorySpec(id, author, filename, executionType, checksum, value);
    }
  }

  private record CanonicalLedgerSpec(String version, String checksum) {}
}
