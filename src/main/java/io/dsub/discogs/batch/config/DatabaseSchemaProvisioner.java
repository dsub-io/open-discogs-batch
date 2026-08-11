package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/** Creates a missing target schema and validates privileges required by batch migrations. */
public final class DatabaseSchemaProvisioner {

  private static final String SCHEMA_EXISTS_SQL =
      "select exists(select 1 from pg_namespace where nspname = ?)";
  private static final String DATABASE_CREATE_PRIVILEGE_SQL =
      "select current_database(), has_database_privilege(current_user, current_database(), 'CREATE')";
  private static final String SCHEMA_PRIVILEGES_SQL =
      "select has_schema_privilege(current_user, ?, 'USAGE') and has_schema_privilege(current_user, ?, 'CREATE')";

  private final DataSource dataSource;
  private final DatabaseSchema schema;

  public DatabaseSchemaProvisioner(DataSource dataSource, DatabaseSchema schema) {
    this.dataSource = dataSource;
    this.schema = schema;
  }

  public boolean ensure() {
    try (Connection connection = dataSource.getConnection()) {
      boolean created = false;
      if (!schemaExists(connection)) {
        requireDatabaseCreatePrivilege(connection);
        createSchema(connection);
        created = true;
      }
      requireSchemaPrivileges(connection);
      return created;
    } catch (SQLException exception) {
      throw new InitializationFailureException(
          "prepare database schema " + schema.name() + ": " + exception.getMessage(), exception);
    }
  }

  private boolean schemaExists(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(SCHEMA_EXISTS_SQL)) {
      statement.setString(1, schema.name());
      try (ResultSet result = statement.executeQuery()) {
        return requiredRow(result, "inspect database schema").getBoolean(1);
      }
    }
  }

  private void requireDatabaseCreatePrivilege(Connection connection) throws SQLException {
    try (PreparedStatement statement =
            connection.prepareStatement(DATABASE_CREATE_PRIVILEGE_SQL);
        ResultSet result = statement.executeQuery()) {
      ResultSet row = requiredRow(result, "inspect database CREATE privilege");
      String databaseName = row.getString(1);
      if (!row.getBoolean(2)) {
        throw new InitializationFailureException(
            "database schema "
                + schema.name()
                + " does not exist and current user lacks CREATE on database "
                + databaseName
                + "; pre-create the schema or grant database CREATE");
      }
    }
  }

  private void createSchema(Connection connection) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          "create schema if not exists " + schema.quotedName() + " authorization current_user");
    }
  }

  private void requireSchemaPrivileges(Connection connection) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(SCHEMA_PRIVILEGES_SQL)) {
      statement.setString(1, schema.name());
      statement.setString(2, schema.name());
      try (ResultSet result = statement.executeQuery()) {
        if (!requiredRow(result, "inspect database schema privileges").getBoolean(1)) {
          throw new InitializationFailureException(
              "current user requires USAGE and CREATE privileges on database schema "
                  + schema.name()
                  + "; grant both privileges or use a writable schema");
        }
      }
    }
  }

  private ResultSet requiredRow(ResultSet result, String operation) throws SQLException {
    if (!result.next()) {
      throw new InitializationFailureException(operation + " returned no result");
    }
    return result;
  }
}
