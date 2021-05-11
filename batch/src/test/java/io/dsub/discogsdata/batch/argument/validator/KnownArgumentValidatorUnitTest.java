package io.dsub.discogsdata.batch.argument.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class KnownArgumentValidatorUnitTest {

  private KnownArgumentValidator validator;

  @BeforeEach
  void setUp() {
    validator = new KnownArgumentValidator();
  }

  @Test
  void validate() {
    ValidationResult result =
        validator.validate(
            new DefaultApplicationArguments("--hello", "--world", "--string", "--chunk", "--t"));

    List<String> issues = result.getIssues();
    assertThat(issues.size()).isEqualTo(3);

    assertThat("unknown argument: string").isIn(issues);
    assertThat("unknown argument: hello").isIn(issues);
    assertThat("unknown argument: world").isIn(issues);

    assertThat("unknown argument: t").isNotIn(issues);
    assertThat("unknown argument: chunk").isNotIn(issues);
  }
}
