package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import io.dsub.opendiscogs.model.manifest.ImportExecution;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class ImportMigrationLockUnitTest {

  private static final String LOCK_SQL = "select pg_try_advisory_lock(?, ?)";
  private static final String UNLOCK_SQL = "select pg_advisory_unlock(?, ?)";
  private static final int LOCK_COUNT =
      ImportExecution.requiredLockEntityTypes(List.of("release")).size();

  private final ImportMigrationLock migrationLock = new ImportMigrationLock();

  @Test
  void acquiresAndReleasesEveryRequiredEntityLockInReverseOrder() throws Exception {
    Fixture fixture = fixture();
    when(fixture.lockStatement().executeQuery()).thenAnswer(invocation -> booleanResult(true));
    when(fixture.unlockStatement().executeQuery()).thenAnswer(invocation -> booleanResult(true));

    try (ImportMigrationLock.Lease ignored = migrationLock.acquire(fixture.connection())) {}

    verify(fixture.lockStatement(), times(LOCK_COUNT)).executeQuery();
    verify(fixture.unlockStatement(), times(LOCK_COUNT)).executeQuery();
  }

  @Test
  void rejectsMissingLockQueryRowsAndActiveImports() throws Exception {
    Fixture missing = fixture();
    ResultSet missingRow = booleanResultWithoutRow();
    when(missing.lockStatement().executeQuery()).thenReturn(missingRow);
    assertThatThrownBy(() -> migrationLock.acquire(missing.connection()))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("lock returned no result");

    Fixture active = fixture();
    ResultSet activeImport = booleanResult(false);
    when(active.lockStatement().executeQuery()).thenReturn(activeImport);
    assertThatThrownBy(() -> migrationLock.acquire(active.connection()))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("import is active");
  }

  @Test
  void releasesPartialAcquisitionAndSuppressesCleanupFailure() throws Exception {
    Fixture fixture = fixture();
    ResultSet acquired = booleanResult(true);
    ResultSet missing = booleanResultWithoutRow();
    when(fixture.lockStatement().executeQuery()).thenReturn(acquired, missing);
    ResultSet failedUnlock = booleanResult(false);
    when(fixture.unlockStatement().executeQuery()).thenReturn(failedUnlock);

    Throwable failure =
        org.assertj.core.api.Assertions.catchThrowable(
            () -> migrationLock.acquire(fixture.connection()));

    assertThat(failure).isInstanceOf(SQLException.class);
    assertThat(failure.getSuppressed()).hasSize(1);
    assertThat(failure.getSuppressed()[0])
        .hasMessageContaining("advisory lock was not held");
    verify(fixture.connection()).abort(org.mockito.ArgumentMatchers.any(Executor.class));
  }

  @Test
  void aClosedConnectionNeedsNoExplicitUnlock() throws Exception {
    Fixture fixture = fixture();
    when(fixture.lockStatement().executeQuery()).thenAnswer(invocation -> booleanResult(true));
    when(fixture.connection().isClosed()).thenReturn(true);

    migrationLock.acquire(fixture.connection()).close();

    verify(fixture.connection(), never()).prepareStatement(UNLOCK_SQL);
  }

  @Test
  void unlockFailsForMissingAndFalseResultRows() throws Exception {
    assertUnlockFailure(booleanResultWithoutRow());
    assertUnlockFailure(booleanResult(false));
  }

  @Test
  void preservesEveryUnlockFailureAndAnyAbortFailure() throws Exception {
    Fixture fixture = fixture();
    when(fixture.lockStatement().executeQuery()).thenAnswer(invocation -> booleanResult(true));
    SQLException unlockFailure = new SQLException("unlock query failed");
    SQLException abortFailure = new SQLException("abort failed");
    when(fixture.unlockStatement().executeQuery()).thenThrow(unlockFailure);
    doThrow(abortFailure).when(fixture.connection())
        .abort(org.mockito.ArgumentMatchers.any(Executor.class));
    ImportMigrationLock.Lease lease = migrationLock.acquire(fixture.connection());

    Throwable failure = org.assertj.core.api.Assertions.catchThrowable(lease::close);

    assertThat(failure).isSameAs(unlockFailure);
    assertThat(failure.getSuppressed()).containsExactly(abortFailure);
  }

  private void assertUnlockFailure(ResultSet unlockResult) throws Exception {
    Fixture fixture = fixture();
    when(fixture.lockStatement().executeQuery()).thenAnswer(invocation -> booleanResult(true));
    when(fixture.unlockStatement().executeQuery()).thenReturn(unlockResult);
    ImportMigrationLock.Lease lease = migrationLock.acquire(fixture.connection());

    assertThatThrownBy(lease::close)
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("advisory lock was not held");
    verify(fixture.connection()).abort(org.mockito.ArgumentMatchers.any(Executor.class));
  }

  private Fixture fixture() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement lockStatement = mock(PreparedStatement.class);
    PreparedStatement unlockStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(LOCK_SQL)).thenReturn(lockStatement);
    when(connection.prepareStatement(UNLOCK_SQL)).thenReturn(unlockStatement);
    return new Fixture(connection, lockStatement, unlockStatement);
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

  private record Fixture(
      Connection connection,
      PreparedStatement lockStatement,
      PreparedStatement unlockStatement) {}
}
