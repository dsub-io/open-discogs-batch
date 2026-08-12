package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Atomically verifies and adopts a model-owned Liquibase history prefix. */
final class LegacyLiquibaseAdopter {

  private static final String CANONICAL_LEDGER_TABLE = "open_discogs_schema_migration";
  private static final String LEGACY_LEDGER_TABLE = "databasechangelog";
  private static final String LEGACY_LOCK_TABLE = "databasechangeloglock";
  private static final String LEGACY_CHANGESET_PATTERN = "open-discogs-model-%";
  private static final String TABLE_EXISTS_SQL =
      "select exists(select 1 from information_schema.tables where table_schema = ? and table_name = ?)";
  private static final String LOCK_TABLE_SQL_TEMPLATE =
      "lock table %s in access exclusive mode nowait";
  private static final String LEGACY_LOCK_STATE_SQL_TEMPLATE =
      "select id, locked from %s order by id";
  private static final String LEGACY_HISTORY_SQL_TEMPLATE =
      "select id, author, filename, exectype, md5sum, orderexecuted from %s where lower(id) like ? order by orderexecuted, id, author, filename";
  private static final String CANONICAL_INSERT_SQL_TEMPLATE =
      "insert into %s (version, checksum) values (?, ?)";
  private static final String FIELD_ID = "id";
  private static final String FIELD_AUTHOR = "author";
  private static final String FIELD_FILENAME = "filename";
  private static final String FIELD_EXECUTION_TYPE = "exectype";
  private static final String FIELD_CHECKSUM = "md5sum";
  private static final int LIQUIBASE_LOCK_ID = 1;

  private final DatabaseSchema schema;
  private final LegacySchemaVerifier schemaVerifier;

  LegacyLiquibaseAdopter(DatabaseSchema schema) {
    this.schema = schema;
    this.schemaVerifier = new LegacySchemaVerifier(schema);
  }

  int adopt(
      Connection connection,
      LegacyLiquibaseContract contract,
      List<CanonicalMigration> canonicalMigrations,
      int canonicalPrefixLength)
      throws SQLException {
    requireLegacyLockTable(connection);
    lockTableNowait(connection, LEGACY_LOCK_TABLE);
    requireInactiveLiquibase(connection);
    lockTableNowait(connection, LEGACY_LEDGER_TABLE);

    List<LegacyHistoryRow> history = readLegacyHistory(connection);
    if (history.isEmpty()) {
      return canonicalPrefixLength;
    }

    LegacySchemaContract schemaContract = contract.schemaContractForLength(history.size());
    LegacySchemaMode schemaMode = schemaMode(history.getFirst(), contract.migrations().getFirst());
    boolean schemaFingerprintRequired =
        validateHistory(history, contract, schemaMode);

    String expectedFingerprint = schemaVerifier.expectedFingerprint(connection, schemaContract);
    if (canonicalPrefixLength >= history.size()) {
      return canonicalPrefixLength;
    }
    if (schemaFingerprintRequired) {
      schemaVerifier.requireMatch(connection, schemaContract.verifierSql(), expectedFingerprint);
    }
    for (int index = canonicalPrefixLength; index < history.size(); index++) {
      recordCanonicalMigration(connection, canonicalMigrations.get(index));
    }
    return history.size();
  }

  private void requireLegacyLockTable(Connection connection) throws SQLException {
    if (!hasTable(connection, LEGACY_LOCK_TABLE)) {
      throw failure("legacy Liquibase ledger exists without its lock table");
    }
  }

  private boolean hasTable(Connection connection, String table) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(TABLE_EXISTS_SQL)) {
      statement.setString(1, schema.name());
      statement.setString(2, table);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new SQLException("legacy table inspection returned no result for " + table);
        }
        return result.getBoolean(1);
      }
    }
  }

  private void lockTableNowait(Connection connection, String table) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(LOCK_TABLE_SQL_TEMPLATE.formatted(qualified(table)));
    }
  }

  private void requireInactiveLiquibase(Connection connection) throws SQLException {
    List<LegacyLockRow> rows = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet result =
            statement.executeQuery(
                LEGACY_LOCK_STATE_SQL_TEMPLATE.formatted(qualified(LEGACY_LOCK_TABLE)))) {
      while (result.next()) {
        rows.add(new LegacyLockRow(result.getInt(1), result.getBoolean(2)));
      }
    }
    if (rows.size() != 1 || rows.getFirst().id() != LIQUIBASE_LOCK_ID) {
      throw failure("legacy Liquibase lock history must contain exactly lock id 1");
    }
    if (rows.getFirst().locked()) {
      throw failure("legacy Liquibase migration is active");
    }
  }

  private List<LegacyHistoryRow> readLegacyHistory(Connection connection) throws SQLException {
    String sql = LEGACY_HISTORY_SQL_TEMPLATE.formatted(qualified(LEGACY_LEDGER_TABLE));
    List<LegacyHistoryRow> rows = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, LEGACY_CHANGESET_PATTERN);
      try (ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          rows.add(
              new LegacyHistoryRow(
                  requiredText(result, 1, FIELD_ID),
                  requiredText(result, 2, FIELD_AUTHOR),
                  requiredText(result, 3, FIELD_FILENAME),
                  requiredText(result, 4, FIELD_EXECUTION_TYPE),
                  result.getString(5),
                  result.getInt(6)));
        }
      }
    }
    return List.copyOf(rows);
  }

  private LegacySchemaMode schemaMode(
      LegacyHistoryRow firstRow, LegacyMigrationContract firstMigration) {
    for (LegacySchemaMode mode : LegacySchemaMode.values()) {
      if (firstMigration.changeSetFor(mode).filename().equals(firstRow.filename())) {
        return mode;
      }
    }
    throw failure("legacy Liquibase history has an unknown changelog filename");
  }

  private boolean validateHistory(
      List<LegacyHistoryRow> history,
      LegacyLiquibaseContract contract,
      LegacySchemaMode schemaMode) {
    boolean schemaFingerprintRequired = false;
    int previousOrder = 0;
    for (int index = 0; index < history.size(); index++) {
      LegacyHistoryRow row = history.get(index);
      LegacyMigrationContract migration = contract.migrations().get(index);
      LegacyChangeSetContract changeSet = migration.changeSetFor(schemaMode);
      if (row.orderExecuted() <= previousOrder) {
        throw failure("legacy Liquibase orderexecuted must be positive and strictly increasing");
      }
      previousOrder = row.orderExecuted();
      requireEqual(changeSet.id(), row.id(), FIELD_ID, index);
      requireEqual(changeSet.author(), row.author(), FIELD_AUTHOR, index);
      requireEqual(changeSet.filename(), row.filename(), FIELD_FILENAME, index);

      LegacyExecutionType executionType = executionType(row.executionType(), index);
      LegacyExecutionPolicy policy = changeSet.policyFor(executionType);
      if (changeSet.checksumRule() instanceof ExactLegacyChecksum exact) {
        requireEqual(exact.checksum(), row.checksum(), FIELD_CHECKSUM, index);
      }
      if (policy.adoptionProof() == LegacyAdoptionProof.SCHEMA_CONTRACT) {
        schemaFingerprintRequired = true;
      }
    }
    return schemaFingerprintRequired;
  }

  private LegacyExecutionType executionType(String value, int index) {
    try {
      return LegacyExecutionType.valueOf(value);
    } catch (IllegalArgumentException exception) {
      throw new InitializationFailureException(
          "legacy Liquibase history has unknown exectype at position " + index + ": " + value,
          exception);
    }
  }

  private void requireEqual(String expected, String actual, String field, int index) {
    if (!expected.equals(actual)) {
      throw failure(
          "legacy Liquibase "
              + field
              + " mismatch at position "
              + index
              + ": expected="
              + expected
              + " actual="
              + actual);
    }
  }

  private void recordCanonicalMigration(Connection connection, CanonicalMigration migration)
      throws SQLException {
    String sql = CANONICAL_INSERT_SQL_TEMPLATE.formatted(qualified(CANONICAL_LEDGER_TABLE));
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, migration.version());
      statement.setString(2, migration.checksum());
      statement.executeUpdate();
    }
  }

  private String requiredText(ResultSet result, int column, String field) throws SQLException {
    String value = result.getString(column);
    if (value == null || value.isBlank()) {
      throw failure("legacy Liquibase " + field + " must not be blank");
    }
    return value;
  }

  private String qualified(String table) {
    return schema.quotedName() + ".\"" + table + '"';
  }

  private InitializationFailureException failure(String message) {
    return new InitializationFailureException(message);
  }

  private record LegacyLockRow(int id, boolean locked) {}

  private record LegacyHistoryRow(
      String id,
      String author,
      String filename,
      String executionType,
      String checksum,
      int orderExecuted) {}
}
