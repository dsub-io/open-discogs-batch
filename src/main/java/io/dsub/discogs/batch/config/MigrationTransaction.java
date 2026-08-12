package io.dsub.discogs.batch.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

/** Executes one migration boundary with deterministic rollback and connection disposal. */
final class MigrationTransaction {

  private static final Executor DIRECT_EXECUTOR = Runnable::run;

  void run(Connection connection, Work work) throws SQLException {
    if (!connection.getAutoCommit()) {
      throw new SQLException("canonical migration connection must start in auto-commit mode");
    }
    connection.setAutoCommit(false);
    Throwable failure = null;
    boolean resolved = false;
    try {
      work.run();
      connection.commit();
      resolved = true;
    } catch (SQLException | RuntimeException | Error exception) {
      failure = exception;
      resolved = rollback(connection, exception);
      if (!resolved) {
        discard(connection, exception);
      }
      throw exception;
    } finally {
      if (resolved) {
        try {
          connection.setAutoCommit(true);
        } catch (SQLException restoreFailure) {
          discard(connection, restoreFailure);
          if (failure == null) {
            throw restoreFailure;
          }
          failure.addSuppressed(restoreFailure);
        }
      }
    }
  }

  private boolean rollback(Connection connection, Throwable failure) {
    try {
      connection.rollback();
      return true;
    } catch (SQLException rollbackFailure) {
      failure.addSuppressed(rollbackFailure);
      return false;
    }
  }

  private void discard(Connection connection, Throwable failure) {
    try {
      connection.abort(DIRECT_EXECUTOR);
    } catch (SQLException abortFailure) {
      failure.addSuppressed(abortFailure);
    }
  }

  @FunctionalInterface
  interface Work {
    void run() throws SQLException;
  }
}
