package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

class MigrationTransactionUnitTest {

  private final MigrationTransaction transaction = new MigrationTransaction();

  @Test
  void commitsAndRestoresAutoCommitAfterSuccess() throws Exception {
    Connection connection = autoCommitConnection();

    transaction.run(connection, () -> {});

    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
    verify(connection, never()).rollback();
  }

  @Test
  void rejectsConnectionsThatAlreadyHaveATransaction() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.getAutoCommit()).thenReturn(false);

    assertThatThrownBy(() -> transaction.run(connection, () -> {}))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("must start in auto-commit mode");
  }

  @Test
  void rollsBackAndRestoresTheConnectionAfterWorkFailure() throws Exception {
    Connection connection = autoCommitConnection();
    SQLException failure = new SQLException("work failed");

    assertThatThrownBy(() -> transaction.run(connection, () -> { throw failure; }))
        .isSameAs(failure);
    verify(connection).rollback();
    verify(connection).setAutoCommit(true);
  }

  @Test
  void rollsBackACommitFailure() throws Exception {
    Connection connection = autoCommitConnection();
    SQLException failure = new SQLException("commit failed");
    doThrow(failure).when(connection).commit();

    assertThatThrownBy(() -> transaction.run(connection, () -> {})).isSameAs(failure);
    verify(connection).rollback();
  }

  @Test
  void discardsTheConnectionWhenRollbackFails() throws Exception {
    Connection connection = autoCommitConnection();
    SQLException workFailure = new SQLException("work failed");
    SQLException rollbackFailure = new SQLException("rollback failed");
    doThrow(rollbackFailure).when(connection).rollback();

    assertThatThrownBy(() -> transaction.run(connection, () -> { throw workFailure; }))
        .isSameAs(workFailure);
    assertThat(workFailure.getSuppressed()).containsExactly(rollbackFailure);
    verify(connection).abort(org.mockito.ArgumentMatchers.any(Executor.class));
    verify(connection, never()).setAutoCommit(true);
  }

  @Test
  void keepsAbortFailureSuppressedBehindTheOriginalFailure() throws Exception {
    Connection connection = autoCommitConnection();
    SQLException workFailure = new SQLException("work failed");
    SQLException rollbackFailure = new SQLException("rollback failed");
    SQLException abortFailure = new SQLException("abort failed");
    doThrow(rollbackFailure).when(connection).rollback();
    doThrow(abortFailure).when(connection)
        .abort(org.mockito.ArgumentMatchers.any(Executor.class));

    assertThatThrownBy(() -> transaction.run(connection, () -> { throw workFailure; }))
        .isSameAs(workFailure);
    assertThat(workFailure.getSuppressed()).containsExactly(rollbackFailure, abortFailure);
  }

  @Test
  void throwsAndDiscardsWhenAutoCommitRestorationFailsAfterSuccess() throws Exception {
    Connection connection = autoCommitConnection();
    SQLException restoreFailure = new SQLException("restore failed");
    doNothing().doThrow(restoreFailure).when(connection).setAutoCommit(org.mockito.ArgumentMatchers.anyBoolean());

    assertThatThrownBy(() -> transaction.run(connection, () -> {})).isSameAs(restoreFailure);
    verify(connection).abort(org.mockito.ArgumentMatchers.any(Executor.class));
  }

  @Test
  void suppressesRestorationFailureBehindTheWorkFailure() throws Exception {
    Connection connection = autoCommitConnection();
    SQLException workFailure = new SQLException("work failed");
    SQLException restoreFailure = new SQLException("restore failed");
    doNothing().doThrow(restoreFailure).when(connection).setAutoCommit(org.mockito.ArgumentMatchers.anyBoolean());

    assertThatThrownBy(() -> transaction.run(connection, () -> { throw workFailure; }))
        .isSameAs(workFailure);
    assertThat(workFailure.getSuppressed()).containsExactly(restoreFailure);
  }

  @Test
  void rollsBackRuntimeFailuresWithoutChangingTheirType() throws Exception {
    Connection connection = autoCommitConnection();
    IllegalStateException failure = new IllegalStateException("runtime failed");

    assertThatThrownBy(() -> transaction.run(connection, () -> { throw failure; }))
        .isSameAs(failure);
    verify(connection).rollback();
  }

  private Connection autoCommitConnection() throws SQLException {
    Connection connection = mock(Connection.class);
    when(connection.getAutoCommit()).thenReturn(true);
    return connection;
  }
}
