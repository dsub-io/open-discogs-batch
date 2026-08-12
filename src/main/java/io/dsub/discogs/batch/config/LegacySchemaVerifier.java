package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/** Verifies the PostgreSQL catalog represented by a legacy schema contract. */
final class LegacySchemaVerifier {

  private static final String POSTGRES_VERSION_SQL =
      "select current_setting('server_version_num')::integer / 10000";
  private static final String SET_SEARCH_PATH_SQL_TEMPLATE = "set local search_path to %s";
  private static final int EXPECTED_COLUMN_COUNT = 1;

  private final DatabaseSchema schema;

  LegacySchemaVerifier(DatabaseSchema schema) {
    this.schema = schema;
  }

  String expectedFingerprint(Connection connection, LegacySchemaContract contract)
      throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(POSTGRES_VERSION_SQL)) {
      if (!result.next()) {
        throw new SQLException("PostgreSQL version query returned no result");
      }
      int major = result.getInt(1);
      if (result.next()) {
        throw new SQLException("PostgreSQL version query returned multiple results");
      }
      return contract.fingerprintForPostgres(major);
    }
  }

  void requireMatch(Connection connection, String verifierSql, String expectedFingerprint)
      throws SQLException {
    setSearchPath(connection);
    String fingerprintInput;
    try (Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(verifierSql)) {
      ResultSetMetaData metadata = result.getMetaData();
      if (metadata.getColumnCount() != EXPECTED_COLUMN_COUNT || !result.next()) {
        throw failure("legacy schema verifier must return exactly one column and one row");
      }
      fingerprintInput = result.getString(1);
      if (fingerprintInput == null || result.next()) {
        throw failure("legacy schema verifier must return one non-null fingerprint input");
      }
    }
    String actualFingerprint = SchemaContractDigest.sha256(fingerprintInput);
    if (!expectedFingerprint.equals(actualFingerprint)) {
      throw failure(
          "legacy schema fingerprint mismatch: expected="
              + expectedFingerprint
              + " actual="
              + actualFingerprint);
    }
  }

  private void setSearchPath(Connection connection) throws SQLException {
    String path = schema.quotedName();
    if (!schema.isPublic()) {
      path += ", \"" + DatabaseSchema.DEFAULT_NAME + "\"";
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute(SET_SEARCH_PATH_SQL_TEMPLATE.formatted(path));
    }
  }

  private InitializationFailureException failure(String message) {
    return new InitializationFailureException(message);
  }
}
