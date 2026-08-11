package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class DatabaseSchemaProvisionerUnitTest {

  @Test
  void existingWritableSchemaIsAccepted() throws Exception {
    Fixture fixture = existingSchema(true);

    assertThat(fixture.provisioner().ensure()).isFalse();
    verify(fixture.connection(), never()).createStatement();
  }

  @Test
  void missingSchemaIsCreatedWhenDatabasePrivilegeAllowsIt() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement existsStatement = mock(PreparedStatement.class);
    PreparedStatement databaseStatement = mock(PreparedStatement.class);
    PreparedStatement privilegeStatement = mock(PreparedStatement.class);
    ResultSet exists = booleanResult(false);
    ResultSet databasePrivilege = mock(ResultSet.class);
    ResultSet schemaPrivileges = booleanResult(true);
    Statement createStatement = mock(Statement.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString()))
        .thenReturn(existsStatement, databaseStatement, privilegeStatement);
    when(existsStatement.executeQuery()).thenReturn(exists);
    when(databaseStatement.executeQuery()).thenReturn(databasePrivilege);
    when(databasePrivilege.next()).thenReturn(true);
    when(databasePrivilege.getString(1)).thenReturn("discogs");
    when(databasePrivilege.getBoolean(2)).thenReturn(true);
    when(connection.createStatement()).thenReturn(createStatement);
    when(privilegeStatement.executeQuery()).thenReturn(schemaPrivileges);

    DatabaseSchemaProvisioner provisioner =
        new DatabaseSchemaProvisioner(dataSource, new DatabaseSchema("open_discogs"));
    assertThat(provisioner.ensure()).isTrue();
    verify(createStatement)
        .executeUpdate(
            "create schema if not exists \"open_discogs\" authorization current_user");
  }

  @Test
  void deniedDatabaseOrSchemaPrivilegesFailWithActionableMessages() throws Exception {
    DataSource deniedDataSource = mock(DataSource.class);
    Connection deniedConnection = mock(Connection.class);
    PreparedStatement existsStatement = mock(PreparedStatement.class);
    PreparedStatement databaseStatement = mock(PreparedStatement.class);
    ResultSet databasePrivilege = mock(ResultSet.class);
    ResultSet missingSchema = booleanResult(false);
    when(deniedDataSource.getConnection()).thenReturn(deniedConnection);
    when(deniedConnection.prepareStatement(anyString()))
        .thenReturn(existsStatement, databaseStatement);
    when(existsStatement.executeQuery()).thenReturn(missingSchema);
    when(databaseStatement.executeQuery()).thenReturn(databasePrivilege);
    when(databasePrivilege.next()).thenReturn(true);
    when(databasePrivilege.getString(1)).thenReturn("discogs");
    when(databasePrivilege.getBoolean(2)).thenReturn(false);

    assertThatThrownBy(
            () ->
                new DatabaseSchemaProvisioner(
                        deniedDataSource, new DatabaseSchema("open_discogs"))
                    .ensure())
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("pre-create the schema");

    Fixture deniedSchema = existingSchema(false);
    assertThatThrownBy(deniedSchema.provisioner()::ensure)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("USAGE and CREATE");
  }

  @Test
  void sqlFailuresKeepTheCause() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    SQLException expected = new SQLException("fixture");
    when(dataSource.getConnection()).thenThrow(expected);

    assertThatThrownBy(
            () ->
                new DatabaseSchemaProvisioner(
                        dataSource, new DatabaseSchema("open_discogs"))
                    .ensure())
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("prepare database schema open_discogs")
        .hasCause(expected);
  }

  @Test
  void missingDatabaseRowsFailClearlyAtEveryQueryBoundary() throws Exception {
    assertNoResultFailure(0, "inspect database schema");
    assertNoResultFailure(1, "inspect database CREATE privilege");
    assertNoResultFailure(2, "inspect database schema privileges");
  }

  private void assertNoResultFailure(int missingResultIndex, String expectedMessage)
      throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement existsStatement = mock(PreparedStatement.class);
    PreparedStatement databaseStatement = mock(PreparedStatement.class);
    PreparedStatement privilegeStatement = mock(PreparedStatement.class);
    when(dataSource.getConnection()).thenReturn(connection);
    if (missingResultIndex == 0) {
      ResultSet noSchemaResult = emptyResult();
      when(connection.prepareStatement(anyString())).thenReturn(existsStatement);
      when(existsStatement.executeQuery()).thenReturn(noSchemaResult);
    } else if (missingResultIndex == 1) {
      ResultSet missingSchema = booleanResult(false);
      ResultSet noDatabasePrivilegeResult = emptyResult();
      when(connection.prepareStatement(anyString()))
          .thenReturn(existsStatement, databaseStatement);
      when(existsStatement.executeQuery()).thenReturn(missingSchema);
      when(databaseStatement.executeQuery()).thenReturn(noDatabasePrivilegeResult);
    } else {
      ResultSet existingSchema = booleanResult(true);
      ResultSet noSchemaPrivilegeResult = emptyResult();
      when(connection.prepareStatement(anyString()))
          .thenReturn(existsStatement, privilegeStatement);
      when(existsStatement.executeQuery()).thenReturn(existingSchema);
      when(privilegeStatement.executeQuery()).thenReturn(noSchemaPrivilegeResult);
    }

    assertThatThrownBy(
            () ->
                new DatabaseSchemaProvisioner(dataSource, new DatabaseSchema("open_discogs"))
                    .ensure())
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining(expectedMessage);
  }

  private Fixture existingSchema(boolean privilegesAllowed) throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    PreparedStatement existsStatement = mock(PreparedStatement.class);
    PreparedStatement privilegeStatement = mock(PreparedStatement.class);
    ResultSet existingSchema = booleanResult(true);
    ResultSet schemaPrivileges = booleanResult(privilegesAllowed);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString()))
        .thenReturn(existsStatement, privilegeStatement);
    when(existsStatement.executeQuery()).thenReturn(existingSchema);
    when(privilegeStatement.executeQuery()).thenReturn(schemaPrivileges);
    return new Fixture(
        connection,
        new DatabaseSchemaProvisioner(dataSource, new DatabaseSchema("open_discogs")));
  }

  private ResultSet booleanResult(boolean value) throws Exception {
    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(true);
    when(result.getBoolean(1)).thenReturn(value);
    return result;
  }

  private ResultSet emptyResult() throws Exception {
    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(false);
    return result;
  }

  private record Fixture(Connection connection, DatabaseSchemaProvisioner provisioner) {}
}
