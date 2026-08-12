package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class LegacySchemaVerifierUnitTest {

  private static final String VERIFIER_SQL = "select fingerprint_input";
  private static final String FINGERPRINT_INPUT = "catalog";
  private static final String EXPECTED_FINGERPRINT =
      "652f55016243bf1b9f1bbea46d5749ef892dbe394e46de9d66ab1aacf0b4af57";

  @Test
  void resolvesTheExpectedFingerprintForTheRunningPostgresMajor() throws Exception {
    VersionFixture fixture = versionFixture(true, false, 18);

    assertThat(
            new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                .expectedFingerprint(fixture.connection(), contract(18)))
        .isEqualTo(EXPECTED_FINGERPRINT);
  }

  @Test
  void rejectsMissingDuplicateAndUnsupportedPostgresVersions() throws Exception {
    VersionFixture missing = versionFixture(false, false, 0);
    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .expectedFingerprint(missing.connection(), contract(18)))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("no result");

    VersionFixture duplicate = versionFixture(true, true, 18);
    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .expectedFingerprint(duplicate.connection(), contract(18)))
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("multiple results");

    VersionFixture unsupported = versionFixture(true, false, 19);
    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .expectedFingerprint(unsupported.connection(), contract(18)))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("does not support PostgreSQL 19");
  }

  @Test
  void verifiesOneExactFingerprintAndUsesTheTargetSearchPath() throws Exception {
    VerificationFixture fixture = verificationFixture(1, true, FINGERPRINT_INPUT, false);
    LegacySchemaVerifier verifier =
        new LegacySchemaVerifier(new DatabaseSchema("open_discogs"));

    verifier.requireMatch(fixture.connection(), VERIFIER_SQL, EXPECTED_FINGERPRINT);

    verify(fixture.searchPathStatement())
        .execute("set local search_path to \"open_discogs\", \"public\"");
    verify(fixture.verifierStatement()).executeQuery(VERIFIER_SQL);
  }

  @Test
  void publicSchemaUsesOneSearchPathEntry() throws Exception {
    VerificationFixture fixture = verificationFixture(1, true, FINGERPRINT_INPUT, false);

    new LegacySchemaVerifier(new DatabaseSchema(DatabaseSchema.DEFAULT_NAME))
        .requireMatch(fixture.connection(), VERIFIER_SQL, EXPECTED_FINGERPRINT);

    verify(fixture.searchPathStatement()).execute("set local search_path to \"public\"");
  }

  @Test
  void rejectsWrongVerifierColumnCountOrMissingRow() throws Exception {
    VerificationFixture wrongColumns = verificationFixture(2, true, FINGERPRINT_INPUT, false);
    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .requireMatch(
                        wrongColumns.connection(), VERIFIER_SQL, EXPECTED_FINGERPRINT))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("exactly one column and one row");

    VerificationFixture noRow = verificationFixture(1, false, null, false);
    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .requireMatch(noRow.connection(), VERIFIER_SQL, EXPECTED_FINGERPRINT))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("exactly one column and one row");
  }

  @Test
  void rejectsNullOrMultipleFingerprintRows() throws Exception {
    VerificationFixture nullValue = verificationFixture(1, true, null, false);
    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .requireMatch(
                        nullValue.connection(), VERIFIER_SQL, EXPECTED_FINGERPRINT))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("one non-null fingerprint input");

    VerificationFixture multiple =
        verificationFixture(1, true, FINGERPRINT_INPUT, true);
    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .requireMatch(
                        multiple.connection(), VERIFIER_SQL, EXPECTED_FINGERPRINT))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("one non-null fingerprint input");
  }

  @Test
  void rejectsAValidlyShapedButDifferentFingerprint() throws Exception {
    VerificationFixture fixture = verificationFixture(1, true, "different", false);

    assertThatThrownBy(
            () ->
                new LegacySchemaVerifier(new DatabaseSchema("open_discogs"))
                    .requireMatch(fixture.connection(), VERIFIER_SQL, EXPECTED_FINGERPRINT))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("legacy schema fingerprint mismatch");
  }

  private VersionFixture versionFixture(boolean firstRow, boolean secondRow, int major)
      throws Exception {
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    ResultSet result = mock(ResultSet.class);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.executeQuery(
            "select current_setting('server_version_num')::integer / 10000"))
        .thenReturn(result);
    when(result.next()).thenReturn(firstRow, secondRow);
    when(result.getInt(1)).thenReturn(major);
    return new VersionFixture(connection);
  }

  private VerificationFixture verificationFixture(
      int columns, boolean firstRow, String value, boolean secondRow) throws Exception {
    Connection connection = mock(Connection.class);
    Statement searchPathStatement = mock(Statement.class);
    Statement verifierStatement = mock(Statement.class);
    ResultSet result = mock(ResultSet.class);
    ResultSetMetaData metadata = mock(ResultSetMetaData.class);
    when(connection.createStatement()).thenReturn(searchPathStatement, verifierStatement);
    when(verifierStatement.executeQuery(VERIFIER_SQL)).thenReturn(result);
    when(result.getMetaData()).thenReturn(metadata);
    when(metadata.getColumnCount()).thenReturn(columns);
    when(result.next()).thenReturn(firstRow, secondRow);
    when(result.getString(1)).thenReturn(value);
    return new VerificationFixture(connection, searchPathStatement, verifierStatement);
  }

  private LegacySchemaContract contract(int postgresMajor) {
    return new LegacySchemaContract(
        "V007",
        List.of("V001", "V002", "V003", "V004", "V005", "V006", "V007"),
        VERIFIER_SQL,
        List.of(new LegacySchemaFingerprint(postgresMajor, EXPECTED_FINGERPRINT)));
  }

  private record VersionFixture(Connection connection) {}

  private record VerificationFixture(
      Connection connection, Statement searchPathStatement, Statement verifierStatement) {}
}
