package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class DefaultDatabaseConnectionValidatorBoundaryUnitTest {

  @Test
  void metadataRejectsUnsupportedProduct() throws Exception {
    DefaultDatabaseConnectionValidator validator = new DefaultDatabaseConnectionValidator();
    DatabaseMetaData unsupported = metadata("unknown", 1, 0);

    assertThat(validator.checkMetaData(unsupported).getIssues())
        .singleElement()
        .asString()
        .contains("unknown", "POSTGRESQL");
  }

  @Test
  void metadataRejectsPostgresql14() throws Exception {
    DefaultDatabaseConnectionValidator validator = new DefaultDatabaseConnectionValidator();

    ValidationResult result = validator.checkMetaData(metadata("PostgreSQL", 14, 0));

    assertThat(result.getIssues())
        .containsExactly("postgresql version below 15 is not supported.");
  }

  @Test
  void metadataAcceptsPostgresql15() throws Exception {
    DefaultDatabaseConnectionValidator validator = new DefaultDatabaseConnectionValidator();

    ValidationResult result = validator.checkMetaData(metadata("PostgreSQL", 15, 0));

    assertThat(result.isValid()).isTrue();
  }

  @Test
  void metadataAcceptsPostgresql18() throws Exception {
    DefaultDatabaseConnectionValidator validator = new DefaultDatabaseConnectionValidator();

    ValidationResult result = validator.checkMetaData(metadata("PostgreSQL", 18, 0));

    assertThat(result.isValid()).isTrue();
  }

  @Test
  void metadataReadFailureReturnsDiagnostic() throws Exception {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    SQLException failure = new SQLException("metadata unavailable", "08006", 17);
    when(metadata.getDatabaseProductName()).thenThrow(failure);

    ValidationResult result =
        new DefaultDatabaseConnectionValidator().checkMetaData(metadata);

    assertThat(result.getIssues())
        .containsExactly(
            "failed to check database metadata by: 08006. errorCode: 17. message: metadata unavailable");
  }

  @Test
  void validateNormalizesNullCredentialsAndReportsConnectionFailures() {
    RecordingValidator validator = new RecordingValidator();

    ValidationResult result = validator.validate("jdbc:postgresql://db/discogs", null, null);

    assertThat(result.getIssues()).containsExactly("failed to test connection! unavailable");
    assertThat(validator.username).isEmpty();
    assertThat(validator.password).isEmpty();
    assertThat(validator.validate(null, "user", "pass").getIssues())
        .containsExactly("url cannot be null or blank");
    assertThat(validator.validate("  ", "user", "pass").getIssues())
        .containsExactly("url cannot be null or blank");
  }

  @Test
  void missingPackagedDriverIsReportedWithoutOpeningAConnection() {
    DefaultDatabaseConnectionValidator validator =
        new DefaultDatabaseConnectionValidator() {
          @Override
          protected void loadDriver(String driverClassName) throws ClassNotFoundException {
            throw new ClassNotFoundException(driverClassName);
          }
        };

    assertThat(validator.validate("jdbc:postgresql://db/discogs", "user", "pass").getIssues())
        .containsExactly("failed to allocate driver for url: jdbc:postgresql://db/discogs");
    assertThat(validator.validate("jdbc:unknown://db/discogs", "user", "pass").getIssues())
        .containsExactly("failed to allocate driver for url: jdbc:unknown://db/discogs");
    assertThat(validator.validate("not-jdbc", "user", "pass").getIssues())
        .containsExactly("failed to allocate driver for url: not-jdbc");
    assertThat(validator.validate("jdbc:://db/discogs", "user", "pass").getIssues())
        .containsExactly("failed to allocate driver for url: jdbc:://db/discogs");
  }

  private DatabaseMetaData metadata(String product, int major, int minor) throws SQLException {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    when(metadata.getDatabaseProductName()).thenReturn(product);
    when(metadata.getDatabaseMajorVersion()).thenReturn(major);
    when(metadata.getDatabaseMinorVersion()).thenReturn(minor);
    return metadata;
  }

  private static final class RecordingValidator extends DefaultDatabaseConnectionValidator {

    private String username;
    private String password;

    @Override
    protected Connection getConnection(String url, String username, String password)
        throws SQLException {
      this.username = username;
      this.password = password;
      throw new SQLException("Unavailable");
    }
  }
}
