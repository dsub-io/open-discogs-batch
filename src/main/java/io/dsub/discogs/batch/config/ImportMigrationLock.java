package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import io.dsub.opendiscogs.model.manifest.ImportExecution;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/** Holds the cross-language entity locks that exclude importers during migration. */
final class ImportMigrationLock {

  private static final String MIGRATION_ENTITY_TYPE = "release";
  private static final String LOCK_SQL = "select pg_try_advisory_lock(?, ?)";
  private static final String UNLOCK_SQL = "select pg_advisory_unlock(?, ?)";
  private static final Executor DIRECT_EXECUTOR = Runnable::run;

  Lease acquire(Connection connection) throws SQLException {
    List<String> lockTypes =
        ImportExecution.requiredLockEntityTypes(List.of(MIGRATION_ENTITY_TYPE));
    List<Integer> acquired = new ArrayList<>(lockTypes.size());
    try (PreparedStatement statement = connection.prepareStatement(LOCK_SQL)) {
      for (String entityType : lockTypes) {
        int key = ImportExecution.entityLockKey(entityType);
        statement.setInt(1, ImportExecution.ADVISORY_LOCK_NAMESPACE);
        statement.setInt(2, key);
        try (ResultSet result = statement.executeQuery()) {
          if (!result.next()) {
            throw new SQLException("schema migration lock returned no result for " + entityType);
          }
          if (!result.getBoolean(1)) {
            throw new InitializationFailureException(
                "cannot migrate schema while an "
                    + entityType
                    + " import is active; stop every Go and Java importer and retry");
          }
        }
        acquired.add(key);
      }
      return new Lease(connection, List.copyOf(acquired), this);
    } catch (SQLException | RuntimeException | Error failure) {
      try {
        release(connection, acquired);
      } catch (SQLException releaseFailure) {
        failure.addSuppressed(releaseFailure);
      }
      throw failure;
    }
  }

  private void release(Connection connection, List<Integer> lockKeys) throws SQLException {
    if (connection.isClosed()) {
      return;
    }
    SQLException failure = null;
    try (PreparedStatement statement = connection.prepareStatement(UNLOCK_SQL)) {
      for (int index = lockKeys.size() - 1; index >= 0; index--) {
        try {
          statement.setInt(1, ImportExecution.ADVISORY_LOCK_NAMESPACE);
          statement.setInt(2, lockKeys.get(index));
          try (ResultSet result = statement.executeQuery()) {
            if (!result.next() || !result.getBoolean(1)) {
              throw new SQLException(
                  "schema migration advisory lock was not held: " + lockKeys.get(index));
            }
          }
        } catch (SQLException exception) {
          if (failure == null) {
            failure = exception;
          } else if (failure != exception) {
            failure.addSuppressed(exception);
          }
        }
      }
    }
    if (failure != null) {
      discard(connection, failure);
      throw failure;
    }
  }

  private void discard(Connection connection, Throwable failure) {
    try {
      connection.abort(DIRECT_EXECUTOR);
    } catch (SQLException abortFailure) {
      failure.addSuppressed(abortFailure);
    }
  }

  static final class Lease implements AutoCloseable {

    private final Connection connection;
    private final List<Integer> lockKeys;
    private final ImportMigrationLock owner;

    private Lease(
        Connection connection, List<Integer> lockKeys, ImportMigrationLock owner) {
      this.connection = connection;
      this.lockKeys = lockKeys;
      this.owner = owner;
    }

    @Override
    public void close() throws SQLException {
      owner.release(connection, lockKeys);
    }
  }
}
