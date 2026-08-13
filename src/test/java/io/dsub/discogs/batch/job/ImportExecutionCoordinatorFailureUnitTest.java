package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.ImportExecutionException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ImportExecutionCoordinatorFailureUnitTest {

  @Test
  void requiredParameterRejectsMissingAndBlankValues() {
    ImportExecutionCoordinator coordinator = coordinator();
    var missing = new org.springframework.batch.core.job.parameters.JobParameters();
    var blank =
        new org.springframework.batch.core.job.parameters.JobParametersBuilder()
            .addString("key", " ")
            .toJobParameters();

    assertThatThrownBy(
            () -> ReflectionTestUtils.invokeMethod(coordinator, "requiredParameter", missing, "key"))
        .hasRootCauseInstanceOf(ImportExecutionException.class);
    assertThatThrownBy(
            () -> ReflectionTestUtils.invokeMethod(coordinator, "requiredParameter", blank, "key"))
        .hasRootCauseInstanceOf(ImportExecutionException.class);
  }

  @Test
  void processorVersionUsesDevelopmentOnlyWhenManifestVersionIsAbsent() {
    ImportExecutionCoordinator coordinator = coordinator();

    org.assertj.core.api.Assertions.assertThat(coordinator.resolveProcessorVersion(null))
        .isEqualTo("development");
    org.assertj.core.api.Assertions.assertThat(coordinator.resolveProcessorVersion("1.2.3"))
        .isEqualTo("1.2.3");
  }

  @Test
  void completionWithAnOpenRunButNoConnectionIsANoOp() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    ReflectionTestUtils.setField(coordinator, "activeRunId", 1L);

    coordinator.complete(true, null);
  }

  @Test
  void completionSqlFailureRollsBackReleasesAndPreservesTheCause() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    Connection connection = mock(Connection.class);
    SQLException failure = new SQLException("fixture completion failure");
    when(connection.prepareStatement(anyString())).thenThrow(failure);
    when(connection.getAutoCommit()).thenReturn(false);
    doThrow(new SQLException("fixture close failure")).when(connection).close();
    ReflectionTestUtils.setField(coordinator, "activeRunId", 1L);
    ReflectionTestUtils.setField(coordinator, "lockConnection", connection);

    assertThatThrownBy(() -> coordinator.complete(true, null))
        .isInstanceOf(ImportExecutionException.class)
        .hasMessage("failed to complete import execution")
        .hasCause(failure);

    verify(connection).rollback();
    verify(connection).close();
  }

  @Test
  void rollbackAndReleaseRemainBestEffortWhenJdbcCleanupFails() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    Connection rollbackFailure = mock(Connection.class);
    when(rollbackFailure.getAutoCommit()).thenThrow(new SQLException("rollback state failure"));
    ReflectionTestUtils.setField(coordinator, "lockConnection", rollbackFailure);
    ReflectionTestUtils.invokeMethod(coordinator, "rollbackQuietly");

    Connection releaseFailure = mock(Connection.class);
    when(releaseFailure.prepareStatement(anyString()))
        .thenThrow(new SQLException("unlock failure"));
    doThrow(new SQLException("close failure")).when(releaseFailure).close();
    ReflectionTestUtils.setField(coordinator, "lockConnection", releaseFailure);
    ReflectionTestUtils.setField(coordinator, "acquiredLockKeys", List.of(1));
    ReflectionTestUtils.invokeMethod(coordinator, "releaseEntityLocks");
    verify(releaseFailure).close();

    ReflectionTestUtils.invokeMethod(coordinator(), "rollbackQuietly");
    ReflectionTestUtils.invokeMethod(coordinator(), "releaseEntityLocks");
  }

  @Test
  void insertRunFailsWhenJdbcDoesNotReturnTheGeneratedIdentity() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet keys = mock(ResultSet.class);
    when(connection.prepareStatement(eq(ImportExecutionQueries.INSERT_RUN), eq(java.sql.Statement.RETURN_GENERATED_KEYS)))
        .thenReturn(statement);
    when(statement.getGeneratedKeys()).thenReturn(keys);
    when(keys.next()).thenReturn(false);
    ReflectionTestUtils.setField(coordinator, "lockConnection", connection);

    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    coordinator, "insertRun", "hash", false, false, "version", null))
        .hasRootCauseInstanceOf(SQLException.class)
        .rootCause()
        .hasMessage("import run insert did not return an ID");
    verify(statement).setNull(6, java.sql.Types.BIGINT);
  }

  @Test
  void dumpInsertFailsWhenNeitherInsertNorLookupReturnsAnIdentity() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    Connection connection = mock(Connection.class);
    PreparedStatement insert = mock(PreparedStatement.class);
    PreparedStatement find = mock(PreparedStatement.class);
    ResultSet insertResult = mock(ResultSet.class);
    ResultSet findResult = mock(ResultSet.class);
    when(connection.prepareStatement(ImportExecutionQueries.INSERT_DUMP)).thenReturn(insert);
    when(connection.prepareStatement(ImportExecutionQueries.FIND_DUMP)).thenReturn(find);
    when(insert.executeQuery()).thenReturn(insertResult);
    when(find.executeQuery()).thenReturn(findResult);
    when(insertResult.next()).thenReturn(false);
    when(findResult.next()).thenReturn(false);
    ReflectionTestUtils.setField(coordinator, "lockConnection", connection);

    Class<?> plannedDumpClass =
        java.util.Arrays.stream(ImportExecutionCoordinator.class.getDeclaredClasses())
            .filter(type -> type.getSimpleName().equals("PlannedDump"))
            .findFirst()
            .orElseThrow();
    Constructor<?> constructor = plannedDumpClass.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    Object plannedDump =
        constructor.newInstance(
            io.dsub.discogs.batch.dump.EntityType.ARTIST,
            LocalDate.of(2026, 7, 1),
            "a".repeat(64),
            1L,
            "etag",
            "uri");

    assertThatThrownBy(
            () -> ReflectionTestUtils.invokeMethod(coordinator, "findOrInsertDump", plannedDump))
        .hasRootCauseInstanceOf(SQLException.class)
        .rootCause()
        .hasMessage("failed to resolve inserted dump artist");
  }

  @Test
  void dumpInsertPreservesResourceCloseFailures() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    Connection connection = mock(Connection.class);
    PreparedStatement insert = mock(PreparedStatement.class);
    ResultSet insertResult = mock(ResultSet.class);
    SQLException bodyFailure = new SQLException("query failure");
    SQLException resultCloseFailure = new SQLException("result close failure");
    SQLException statementCloseFailure = new SQLException("statement close failure");
    when(connection.prepareStatement(ImportExecutionQueries.INSERT_DUMP)).thenReturn(insert);
    when(insert.executeQuery()).thenReturn(insertResult);
    when(insertResult.next()).thenThrow(bodyFailure);
    doThrow(resultCloseFailure).when(insertResult).close();
    doThrow(statementCloseFailure).when(insert).close();
    ReflectionTestUtils.setField(coordinator, "lockConnection", connection);
    Object plannedDump = plannedDump();

    assertThatThrownBy(
            () -> ReflectionTestUtils.invokeMethod(coordinator, "findOrInsertDump", plannedDump))
        .hasRootCause(bodyFailure);
    org.assertj.core.api.Assertions.assertThat(bodyFailure.getSuppressed())
        .containsExactly(resultCloseFailure, statementCloseFailure);
  }

  @Test
  void dumpInsertRejectsNullJdbcResources() throws Exception {
    ImportExecutionCoordinator nullResultCoordinator = coordinator();
    Connection resultConnection = mock(Connection.class);
    PreparedStatement insert = mock(PreparedStatement.class);
    when(resultConnection.prepareStatement(ImportExecutionQueries.INSERT_DUMP)).thenReturn(insert);
    when(insert.executeQuery()).thenReturn(null);
    ReflectionTestUtils.setField(nullResultCoordinator, "lockConnection", resultConnection);

    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    nullResultCoordinator, "findOrInsertDump", plannedDump()))
        .isInstanceOf(NullPointerException.class);

    ImportExecutionCoordinator nullStatementCoordinator = coordinator();
    Connection statementConnection = mock(Connection.class);
    when(statementConnection.prepareStatement(ImportExecutionQueries.INSERT_DUMP)).thenReturn(null);
    ReflectionTestUtils.setField(nullStatementCoordinator, "lockConnection", statementConnection);

    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    nullStatementCoordinator, "findOrInsertDump", plannedDump()))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void bootstrapOperationRejectsMissingResultAndPreservesSqlFailure() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet emptyResult = mock(ResultSet.class);
    when(connection.prepareStatement(ImportExecutionQueries.FINALIZE_BOOTSTRAP))
        .thenReturn(statement);
    when(statement.executeQuery()).thenReturn(emptyResult);
    when(emptyResult.next()).thenReturn(false);
    ReflectionTestUtils.setField(coordinator, "lockConnection", connection);

    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    coordinator,
                    "runBootstrapOperation",
                    ImportExecutionQueries.FINALIZE_BOOTSTRAP,
                    7L,
                    "finalize"))
        .hasRootCauseInstanceOf(ImportExecutionException.class)
        .rootCause()
        .hasMessage("finalize import run 7 bootstrap returned no result");

    SQLException failure = new SQLException("fixture bootstrap failure");
    when(statement.executeQuery()).thenThrow(failure);
    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    coordinator,
                    "runBootstrapOperation",
                    ImportExecutionQueries.FINALIZE_BOOTSTRAP,
                    7L,
                    "finalize"))
        .hasCauseInstanceOf(ImportExecutionException.class)
        .cause()
        .hasMessage("finalize import run 7 bootstrap")
        .hasCause(failure);
  }

  private Object plannedDump() throws Exception {
    Class<?> plannedDumpClass =
        java.util.Arrays.stream(ImportExecutionCoordinator.class.getDeclaredClasses())
            .filter(type -> type.getSimpleName().equals("PlannedDump"))
            .findFirst()
            .orElseThrow();
    Constructor<?> constructor = plannedDumpClass.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    return constructor.newInstance(
        io.dsub.discogs.batch.dump.EntityType.ARTIST,
        LocalDate.of(2026, 7, 1),
        "a".repeat(64),
        1L,
        "etag",
        "uri");
  }

  @Test
  void resumeTransferRejectsMissingSummariesAndChunkCountMismatch() throws Exception {
    ImportExecutionCoordinator coordinator = coordinator();
    Connection connection = mock(Connection.class);
    PreparedStatement summaries = mock(PreparedStatement.class);
    when(connection.prepareStatement(ImportExecutionQueries.COPY_RESUME_SUMMARIES))
        .thenReturn(summaries);
    when(summaries.executeUpdate()).thenReturn(0);
    ReflectionTestUtils.setField(coordinator, "lockConnection", connection);

    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    coordinator, "transferResumeProgress", 1L, 2L, 1))
        .hasRootCauseInstanceOf(ImportExecutionException.class)
        .rootCause()
        .hasMessageContaining("copied 0 of 1 entity summaries");

    PreparedStatement chunks = mock(PreparedStatement.class);
    PreparedStatement delete = mock(PreparedStatement.class);
    when(summaries.executeUpdate()).thenReturn(1);
    when(connection.prepareStatement(ImportExecutionQueries.COPY_RESUME_CHUNKS))
        .thenReturn(chunks);
    when(connection.prepareStatement(ImportExecutionQueries.DELETE_RUN_CHUNKS))
        .thenReturn(delete);
    when(chunks.executeUpdate()).thenReturn(2);
    when(delete.executeUpdate()).thenReturn(1);

    assertThatThrownBy(
            () ->
                ReflectionTestUtils.invokeMethod(
                    coordinator, "transferResumeProgress", 1L, 2L, 1))
        .hasRootCauseInstanceOf(ImportExecutionException.class)
        .rootCause()
        .hasMessageContaining("copied 2 but pruned 1 chunks");
  }

  private ImportExecutionCoordinator coordinator() {
    return new ImportExecutionCoordinator(mock(DataSource.class));
  }
}
