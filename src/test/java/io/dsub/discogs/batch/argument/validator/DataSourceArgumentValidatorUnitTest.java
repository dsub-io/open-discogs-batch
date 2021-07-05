package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class DataSourceArgumentValidatorUnitTest {

  private final DataSourceArgumentValidator validator = new DataSourceArgumentValidator();

  void innerTestCorrectJdbcUrl(String jdbcUrl) {
    String[] args = new String[]{jdbcUrl, "user=hello", "pass=pass"};
    ValidationResult result = validator.validate(new DefaultApplicationArguments(args));
    assertThat(result.isValid()).isEqualTo(true);
  }

  void innerTestMalformedJdbcUrl(String jdbcUrl, String expectedReport) {
    String[] args = new String[]{jdbcUrl, "user=hello", "pass=pass"};
    ValidationResult result = validator.validate(new DefaultApplicationArguments(args));
    assertThat(result.isValid()).isEqualTo(false);
    assertThat(expectedReport).isIn(result.getIssues());
  }

  @Test
  void shouldReportEveryMissingFields() {
    String[] arg = new String[0];
    ValidationResult result = validator.validate(new DefaultApplicationArguments(arg));

    assertThat(result).returns(false, ValidationResult::isValid);
    assertThat(result.getIssues().size()).isEqualTo(3);

    List<String> issues = result.getIssues();

    assertThat("url argument is missing").isIn(issues);
    assertThat("username argument is missing").isIn(issues);
    assertThat("password argument is missing").isIn(issues);

    arg = new String[]{"--user=hello"};

    result = validator.validate(new DefaultApplicationArguments(arg));
    assertThat(result).returns(false, ValidationResult::isValid);
    assertThat(result.getIssues().size()).isEqualTo(2);
    issues = result.getIssues();

    assertThat("url argument is missing").isIn(issues);
    assertThat("password argument is missing").isIn(issues);

    arg = new String[]{"--user=hello", "--pass=password", "--hello=world"};
    result = validator.validate(new DefaultApplicationArguments(arg));
    assertThat(result).returns(false, ValidationResult::isValid);
    assertThat(result.getIssues().size()).isEqualTo(1);
    issues = result.getIssues();

    assertThat("url argument is missing").isIn(issues);
  }

  @Test
  void shouldReportEveryDuplicatedEntries() {
    String[] arg = new String[]{"--user=hello", "--user=world", "--pass=hi", "--url=333"};
    ValidationResult result = validator.validate(new DefaultApplicationArguments(arg));

    assertThat(result).returns(false, ValidationResult::isValid);
    assertThat(result.getIssues().size()).isEqualTo(1);

    List<String> issues = result.getIssues();
    assertThat("username argument has duplicated entries").isIn(issues);

    arg = new String[]{"--user=hello", "--user=world", "--url=what", "--url=where", "--pass=eee"};
    result = validator.validate(new DefaultApplicationArguments(arg));

    assertThat(result).returns(false, ValidationResult::isValid);
    assertThat(result.getIssues().size()).isEqualTo(2);

    issues = result.getIssues();
    assertThat("username argument has duplicated entries").isIn(issues);
    assertThat("url argument has duplicated entries").isIn(issues);
  }

  @Test
  void shouldReportAsBlankIfEverythingIsPresent() {
    String[] arg =
        new String[]{"--user=hello", "--pass=pass", "--url=jdbc:mysql://localhost:3306/something"};
    ValidationResult result = validator.validate(new DefaultApplicationArguments(arg));
    assertThat(result).returns(true, ValidationResult::isValid);
    assertThat(result.getIssues().size()).isEqualTo(0);
  }
}
