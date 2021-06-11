package io.dsub.discogs.batch.argument.validator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.dsub.discogs.common.exception.MissingRequiredParamsException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class CompositeArgumentValidatorUnitTest {

  private CompositeArgumentValidator compositeArgumentValidator;
  private ArgumentValidator helloValidator;
  private ArgumentValidator worldValidator;

  @BeforeEach
  void setUp() {
    this.compositeArgumentValidator = new CompositeArgumentValidator();
    this.helloValidator = arg -> new DefaultValidationResult("hello");
    this.worldValidator = arg -> new DefaultValidationResult("world");
  }

  @Test
  void addValidator() {
    assertThat(this.compositeArgumentValidator.addValidator(helloValidator))
        .isEqualTo(this.compositeArgumentValidator);
    assertThat(this.compositeArgumentValidator.addValidator(worldValidator))
        .isEqualTo(this.compositeArgumentValidator);
  }

  @Test
  void addValidators() {
    this.compositeArgumentValidator = new CompositeArgumentValidator();
    assertThat(this.compositeArgumentValidator.addValidators(helloValidator, worldValidator))
        .isEqualTo(this.compositeArgumentValidator);
    assertThat(
            this.compositeArgumentValidator.addValidators(List.of(helloValidator, worldValidator)))
        .isEqualTo(this.compositeArgumentValidator);
    List<ArgumentValidator> argumentValidators = null;
    assertThat(this.compositeArgumentValidator.addValidators(argumentValidators))
        .isEqualTo(this.compositeArgumentValidator);
    ArgumentValidator[] validators = null;
    assertThat(this.compositeArgumentValidator.addValidators(validators))
        .isEqualTo(this.compositeArgumentValidator);
  }

  @Test
  void validate() {
    this.compositeArgumentValidator.addValidators(helloValidator, worldValidator);
    ValidationResult result = this.compositeArgumentValidator.validate(null);
    assertThat(result.isValid()).isFalse();
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat(result.getIssues().get(0)).isEqualTo("applicationArgument cannot be null");

    result = this.compositeArgumentValidator.validate(new DefaultApplicationArguments());
    assertThat(result.isValid()).isFalse();
    assertThat(result.getIssues().size()).isEqualTo(2);
    assertThat(result.getIssues().get(0)).isEqualTo("hello");
    assertThat(result.getIssues().get(1)).isEqualTo("world");
  }

  @Test
  void afterPropertiesSet() {
    assertThrows(
        MissingRequiredParamsException.class,
        () -> this.compositeArgumentValidator.afterPropertiesSet());
    this.compositeArgumentValidator.addValidator(helloValidator);
    assertDoesNotThrow(() -> this.compositeArgumentValidator.afterPropertiesSet());
  }
}
