package io.dsub.discogsdata.batch.argument.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationResultUnitTest {

  private DefaultValidationResult validationResult;

  @BeforeEach
  void setUp() {
    this.validationResult = new DefaultValidationResult();
  }

  @Test
  void combinedMethodShouldReturnUniqueCopy() {
    DefaultValidationResult otherResult = new DefaultValidationResult();
    otherResult = otherResult.withIssues("hello", "world");
    DefaultValidationResult combined = this.validationResult.combine(otherResult);
    assertThat(this.validationResult.getIssues().size()).isEqualTo(0);
    assertThat(combined.getIssues().size()).isEqualTo(2);
    assertThat(combined).isNotEqualTo(this.validationResult);
    assertThat(combined.getIssues()).isNotEqualTo(this.validationResult.getIssues());

    otherResult = new DefaultValidationResult("whatever");

    ValidationResult otherCombined = combined.combine(otherResult);

    assertThat(otherCombined.getIssues().size()).isEqualTo(3);
    assertThat(otherCombined).isNotEqualTo(combined);
    assertThat(otherCombined.getIssues()).isNotEqualTo(combined.getIssues());
    assertThat("whatever").isIn(otherCombined.getIssues());
    assertThat("hello").isIn(otherCombined.getIssues());
    assertThat("world").isIn(otherCombined.getIssues());
  }

  @Test
  void testConstructors() {
    assertThat(this.validationResult.getIssues()).isNotNull();
    assertThat(this.validationResult.getIssues().size()).isEqualTo(0);

    ValidationResult result = new DefaultValidationResult("hello");

    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat("hello").isIn(result.getIssues());

    result = new DefaultValidationResult("hello", "world");
    assertThat(result.getIssues()).isNotNull();
    assertThat(result.getIssues().size()).isEqualTo(2);
    assertThat("hello").isIn(result.getIssues());
    assertThat("world").isIn(result.getIssues());

    result = new DefaultValidationResult(List.of("what", "ever"));
    assertThat(result.getIssues()).isNotNull();
    assertThat(result.getIssues().size()).isEqualTo(2);
    assertThat("what").isIn(result.getIssues());
    assertThat("ever").isIn(result.getIssues());
  }

  @Test
  void testConstructorsWithNullParams() {
    List<String> nullList = null;
    this.validationResult = new DefaultValidationResult(nullList);
    assertThat(this.validationResult.isValid()).isTrue();
    assertThat(this.validationResult.getIssues()).isNotNull();

    String nullString = null;
    this.validationResult = new DefaultValidationResult(nullString);
    assertThat(this.validationResult.isValid()).isTrue();
    assertThat(this.validationResult.getIssues()).isNotNull();

    String[] nullStringArray = null;
    this.validationResult = new DefaultValidationResult(nullStringArray);
    assertThat(this.validationResult.isValid()).isTrue();
    assertThat(this.validationResult.getIssues()).isNotNull();
  }

  @Test
  void testWithIssues() {
    DefaultValidationResult other = this.validationResult.withIssues("hello", "world");
    assertThat(this.validationResult).isNotEqualTo(other);
    assertThat(other.getIssues()).isNotEqualTo(this.validationResult.getIssues());
    assertThat(other.getIssues().size()).isEqualTo(2);

    DefaultValidationResult another = other.withIssues("hi", "there");
    assertThat(another.getIssues().size()).isEqualTo(4);
    assertThat("hi").isIn(another.getIssues());
    assertThat("there").isIn(another.getIssues());

    ValidationResult yetAnother = another.withIssues(List.of("orange"));
    assertThat(yetAnother.getIssues().size()).isEqualTo(5);
    assertThat("orange").isIn(yetAnother.getIssues());
  }

  @Test
  void addingNullShouldNotImpact() {
    validationResult = validationResult.withIssues("hello");
    String s = null;

    ValidationResult other = validationResult.withIssues(s);
    assertThat(other.getIssues().size()).isEqualTo(1);
    assertThat(other.getIssues()).isEqualTo(validationResult.getIssues());

    List<String> list = null;
    other = validationResult.withIssues(list);
    assertThat(validationResult.getIssues().size()).isEqualTo(1);
    assertThat(other.getIssues()).isEqualTo(validationResult.getIssues());
  }

  @Test
  void testWithIssue() {
    DefaultValidationResult result = this.validationResult.withIssue("hi");
    assertThat(this.validationResult.getIssues().size()).isEqualTo(0);
    assertThat(result.getIssues().size()).isEqualTo(1);
    assertThat("hi").isIn(result.getIssues());

    ValidationResult another = result.withIssue("whatever");
    assertThat(another.getIssues()).isNotEqualTo(result.getIssues());
    assertThat(another.getIssues().size()).isEqualTo(2);
  }

  @Test
  void isValid() {
    // initialized as an empty one.
    assertThat(this.validationResult.isValid()).isTrue();

    // adds an issue
    this.validationResult = this.validationResult.withIssues("hi");
    assertThat(this.validationResult.isValid()).isFalse();
  }

  @Test
  void getIssuesShouldReturnNotNullUnmodifiableList() {
    assertThat(this.validationResult.getIssues()).isNotNull();

    this.validationResult = this.validationResult.withIssues("hi");
    assertThat(this.validationResult.getIssues()).isNotNull();

    assertThrows(Exception.class, () -> this.validationResult.getIssues().add("hi"));
  }
}
