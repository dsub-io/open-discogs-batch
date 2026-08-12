package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class CanonicalSchemaMigratorUnitTest {

  private static final String IMPORT_LOCK_SQL = "select pg_try_advisory_lock(?, ?)";
  private static final String IMPORT_UNLOCK_SQL = "select pg_advisory_unlock(?, ?)";
  private static final String BOOTSTRAP_LOCK_SQL =
      "select pg_try_advisory_xact_lock(hashtextextended(current_database() || ':' || ?, 0))";
  private static final String TABLE_EXISTS_SQL =
      "select exists(select 1 from information_schema.tables where table_schema = ? and table_name = ?)";

  @Test
  void rejectsMissingAndUnavailableBootstrapLockResults() throws Exception {
    Fixture missing = fixture(booleanResultWithoutRow(), booleanResult(false));
    assertThatThrownBy(missing.migrator()::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("another schema migrator is active");

    Fixture unavailable = fixture(booleanResult(false), booleanResult(false));
    assertThatThrownBy(unavailable.migrator()::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("another schema migrator is active");
  }

  @Test
  void wrapsMissingTableInspectionResultsAsInitializationFailure() throws Exception {
    Fixture fixture = fixture(booleanResult(true), booleanResultWithoutRow());
    Statement createLedger = mock(Statement.class);
    when(fixture.connection().createStatement()).thenReturn(createLedger);

    assertThatThrownBy(fixture.migrator()::migrate)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("table inspection returned no result");
  }

  private Fixture fixture(ResultSet bootstrapResult, ResultSet tableResult) throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    CanonicalMigrationSource migrationSource = mock(CanonicalMigrationSource.class);
    LegacyLiquibaseContractSource contractSource = mock(LegacyLiquibaseContractSource.class);
    PreparedStatement importLock = mock(PreparedStatement.class);
    PreparedStatement importUnlock = mock(PreparedStatement.class);
    PreparedStatement bootstrapLock = mock(PreparedStatement.class);
    PreparedStatement tableInspection = mock(PreparedStatement.class);
    List<CanonicalMigration> migrations = List.of();
    LegacyLiquibaseContract contract =
        new LegacyLiquibaseContract(List.of(), List.of());

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getAutoCommit()).thenReturn(true);
    when(migrationSource.load()).thenReturn(migrations);
    when(contractSource.load(migrations)).thenReturn(contract);
    when(connection.prepareStatement(IMPORT_LOCK_SQL)).thenReturn(importLock);
    when(connection.prepareStatement(IMPORT_UNLOCK_SQL)).thenReturn(importUnlock);
    when(connection.prepareStatement(BOOTSTRAP_LOCK_SQL)).thenReturn(bootstrapLock);
    when(connection.prepareStatement(TABLE_EXISTS_SQL)).thenReturn(tableInspection);
    when(importLock.executeQuery()).thenAnswer(invocation -> booleanResult(true));
    when(importUnlock.executeQuery()).thenAnswer(invocation -> booleanResult(true));
    when(bootstrapLock.executeQuery()).thenReturn(bootstrapResult);
    when(tableInspection.executeQuery()).thenReturn(tableResult);

    CanonicalSchemaMigrator migrator =
        new CanonicalSchemaMigrator(
            dataSource,
            new DatabaseSchema("open_discogs"),
            migrationSource,
            contractSource);
    return new Fixture(connection, migrator);
  }

  private ResultSet booleanResult(boolean value) throws SQLException {
    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(true);
    when(result.getBoolean(1)).thenReturn(value);
    return result;
  }

  private ResultSet booleanResultWithoutRow() throws SQLException {
    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(false);
    return result;
  }

  private record Fixture(Connection connection, CanonicalSchemaMigrator migrator) {}
}
