package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

/** Applies immutable model migrations through the cross-language checksum ledger. */
public final class CanonicalSchemaMigrator {

  private static final String MIGRATION_LEDGER_TABLE = "open_discogs_schema_migration";
  private static final String LEGACY_LEDGER_TABLE = "databasechangelog";
  private static final String CANONICAL_SCHEMA_PREFIX = "public.";
  private static final String MIGRATION_BOOTSTRAP_LOCK_NAME =
      "open-discogs-schema-migration";
  private static final String MIGRATION_BOOTSTRAP_LOCK_SQL =
      "select pg_try_advisory_xact_lock(hashtextextended(current_database() || ':' || ?, 0))";
  private static final String TABLE_EXISTS_SQL =
      "select exists(select 1 from information_schema.tables where table_schema = ? and table_name = ?)";
  private static final String EXTENSION_SCHEMA_SQL =
      "select namespace.nspname, quote_ident(namespace.nspname) from pg_extension extension join pg_namespace namespace on namespace.oid = extension.extnamespace where extension.extname = 'pg_trgm'";

  private final DataSource dataSource;
  private final DatabaseSchema schema;
  private final CanonicalMigrationSource migrationSource;
  private final LegacyLiquibaseContractSource legacyContractSource;
  private final LegacyLiquibaseAdopter legacyAdopter;
  private final ImportMigrationLock importLock;
  private final MigrationTransaction transaction;

  public CanonicalSchemaMigrator(DataSource dataSource, DatabaseSchema schema) {
    this(
        dataSource,
        schema,
        new CanonicalMigrationSource(),
        new LegacyLiquibaseContractSource());
  }

  CanonicalSchemaMigrator(
      DataSource dataSource,
      DatabaseSchema schema,
      CanonicalMigrationSource migrationSource,
      LegacyLiquibaseContractSource legacyContractSource) {
    this.dataSource = dataSource;
    this.schema = schema;
    this.migrationSource = migrationSource;
    this.legacyContractSource = legacyContractSource;
    this.legacyAdopter = new LegacyLiquibaseAdopter(schema);
    this.importLock = new ImportMigrationLock();
    this.transaction = new MigrationTransaction();
  }

  /** Applies every canonical migration exactly once in model-defined order. */
  public void migrate() {
    List<CanonicalMigration> migrations = migrationSource.load();
    LegacyLiquibaseContract legacyContract = legacyContractSource.load(migrations);
    try (Connection connection = dataSource.getConnection();
        ImportMigrationLock.Lease ignored = importLock.acquire(connection)) {
      createLedger(connection);
      if (hasTable(connection, LEGACY_LEDGER_TABLE)) {
        adoptLegacyHistory(connection, migrations, legacyContract);
      } else {
        validateLedger(connection, migrations);
      }
      for (CanonicalMigration migration : migrations) {
        applyMigration(connection, migration);
      }
    } catch (SQLException exception) {
      throw failure("apply canonical schema migrations", exception);
    }
  }

  private void adoptLegacyHistory(
      Connection connection,
      List<CanonicalMigration> migrations,
      LegacyLiquibaseContract legacyContract)
      throws SQLException {
    transaction.run(
        connection,
        () -> {
          lockLedger(connection);
          int canonicalPrefixLength = validateLedger(connection, migrations);
          legacyAdopter.adopt(
              connection,
              legacyContract,
              migrations,
              canonicalPrefixLength);
        });
  }

  private void createLedger(Connection connection) throws SQLException {
    transaction.run(
        connection,
        () -> {
      try (PreparedStatement lock =
          connection.prepareStatement(MIGRATION_BOOTSTRAP_LOCK_SQL)) {
        lock.setString(1, MIGRATION_BOOTSTRAP_LOCK_NAME + ':' + schema.name());
        try (ResultSet result = lock.executeQuery()) {
          if (!result.next() || !result.getBoolean(1)) {
            throw new InitializationFailureException(
                "another schema migrator is active for schema " + schema.name());
          }
        }
      }
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate(
            """
            create table if not exists %s
            (
                version     varchar(255) primary key,
                checksum    char(64) not null,
                applied_at  timestamp not null default now()
            )
            """
                .formatted(qualified(MIGRATION_LEDGER_TABLE)));
      }
        });
  }

  private boolean hasTable(Connection connection, String table) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            TABLE_EXISTS_SQL)) {
      statement.setString(1, schema.name());
      statement.setString(2, table);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new SQLException("table inspection returned no result for " + table);
        }
        return result.getBoolean(1);
      }
    }
  }

  private void applyMigration(Connection connection, CanonicalMigration migration)
      throws SQLException {
    transaction.run(
        connection,
        () -> {
          lockLedger(connection);
          String appliedChecksum = readAppliedChecksum(connection, migration.version());
          if (appliedChecksum != null) {
            requireMatchingChecksum(migration, appliedChecksum);
          } else {
            executeMigration(connection, migration);
            recordMigration(connection, migration);
          }
        });
  }

  private int validateLedger(Connection connection, List<CanonicalMigration> migrations)
      throws SQLException {
    List<MigrationLedgerRow> applied = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet result =
            statement.executeQuery(
                "select version, checksum from "
                    + qualified(MIGRATION_LEDGER_TABLE)
                    + " order by version")) {
      while (result.next()) {
        applied.add(new MigrationLedgerRow(result.getString(1), result.getString(2)));
      }
    }
    if (applied.size() > migrations.size()) {
      throw new InitializationFailureException(
          "database schema contains migrations newer than this batch artifact");
    }
    for (int index = 0; index < applied.size(); index++) {
      MigrationLedgerRow row = applied.get(index);
      CanonicalMigration expected = migrations.get(index);
      if (!row.version().equals(expected.version())) {
        throw new InitializationFailureException(
            "database migration history is not a canonical prefix: position="
                + index
                + " database="
                + row.version()
                + " artifact="
                + expected.version());
      }
      requireMatchingChecksum(expected, row.checksum());
    }
    return applied.size();
  }

  private void lockLedger(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "lock table " + qualified(MIGRATION_LEDGER_TABLE) + " in exclusive mode");
    }
  }

  private String readAppliedChecksum(Connection connection, String version) throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "select checksum from "
                + qualified(MIGRATION_LEDGER_TABLE)
                + " where version = ?")) {
      statement.setString(1, version);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? result.getString(1) : null;
      }
    }
  }

  private void requireMatchingChecksum(
      CanonicalMigration migration, String appliedChecksum) {
    if (!migration.checksum().equals(appliedChecksum)) {
      throw new InitializationFailureException(
          "canonical migration "
              + migration.version()
              + " checksum changed: database="
              + appliedChecksum
              + " artifact="
              + migration.checksum());
    }
  }

  private void executeMigration(Connection connection, CanonicalMigration migration)
      throws SQLException {
    setMigrationSearchPath(connection);
    String scopedSql = migration.sql().replace(CANONICAL_SCHEMA_PREFIX, schema.quotedName() + '.');
    try (Statement statement = connection.createStatement()) {
      statement.execute(scopedSql);
    }
  }

  private void setMigrationSearchPath(Connection connection) throws SQLException {
    List<String> path = new ArrayList<>(3);
    Set<String> pathNames = new HashSet<>(3);
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(EXTENSION_SCHEMA_SQL)) {
      if (result.next()) {
        appendSearchPath(path, pathNames, result.getString(1), result.getString(2));
      } else {
        appendSearchPath(
            path,
            pathNames,
            DatabaseSchema.DEFAULT_NAME,
            '"' + DatabaseSchema.DEFAULT_NAME + '"');
      }
    }
    appendSearchPath(path, pathNames, schema.name(), schema.quotedName());
    appendSearchPath(
        path,
        pathNames,
        DatabaseSchema.DEFAULT_NAME,
        '"' + DatabaseSchema.DEFAULT_NAME + '"');
    try (Statement statement = connection.createStatement()) {
      statement.execute("set local search_path to " + String.join(", ", path));
    }
  }

  private void appendSearchPath(
      List<String> path, Set<String> pathNames, String name, String identifier) {
    if (pathNames.add(name)) {
      path.add(identifier);
    }
  }

  private void recordMigration(Connection connection, CanonicalMigration migration)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "insert into "
                + qualified(MIGRATION_LEDGER_TABLE)
                + " (version, checksum) values (?, ?)")) {
      statement.setString(1, migration.version());
      statement.setString(2, migration.checksum());
      statement.executeUpdate();
    }
  }

  private String qualified(String table) {
    return schema.quotedName() + ".\"" + table + '"';
  }

  private InitializationFailureException failure(String operation, Exception cause) {
    return new InitializationFailureException(
        operation + " in schema " + schema.name() + ": " + cause.getMessage(), cause);
  }

  private record MigrationLedgerRow(String version, String checksum) {}

}
