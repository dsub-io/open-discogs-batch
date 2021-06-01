package io.dsub.discogsdata.batch.argument.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * An immutable implementation for the {@link ValidationResult}. The issues will only be accumulated
 * during each validation step. The content and the instance will be virtually immutable.
 */
public class DefaultValidationResult implements ValidationResult {

  /**
   * Accumulated issues. Should always be maintained immutable from external access.
   */
  private final Collection<String> issues;

  /**
   * Default no argument constructor.
   */
  public DefaultValidationResult() {
    this.issues = new ArrayList<>();
  }

  /**
   * Initializes itself with a given issues.
   *
   * @param issues predefined issues as initial values.
   */
  public DefaultValidationResult(Collection<String> issues) {
    if (issues == null) {
      this.issues = new ArrayList<>();
      return;
    }
    this.issues = issues;
  }

  /**
   * Initializes with single issue.
   *
   * @param issue predefined issue as an initial value.
   */
  public DefaultValidationResult(String issue) {
    if (issue == null) {
      this.issues = new ArrayList<>();
      return;
    }
    this.issues = List.of(issue);
  }

  /**
   * Initializes with multiple issues.
   *
   * @param issues can be null, empty or multiple issues as a set of initial value.
   */
  public DefaultValidationResult(String... issues) {
    if (issues == null || issues.length == 0) {
      this.issues = new ArrayList<>();
      return;
    }
    this.issues = List.of(issues);
  }

  /**
   * Accumulates itself to be combined with other issue.
   *
   * @param issue additional issue to be accumulated.
   * @return a new instance of itself with the additional value.
   */
  @Override
  public DefaultValidationResult withIssue(String issue) {
    if (issue == null) {
      return new DefaultValidationResult(List.copyOf(this.issues));
    }
    List<String> list = new ArrayList<>(this.issues);
    list.add(issue);
    return new DefaultValidationResult(List.copyOf(list));
  }

  /**
   * Accumulates itself to be combined with other issues.
   *
   * @param issues additional issues to be accumulated.
   * @return a new instance of itself with the additional value.
   */
  @Override
  public DefaultValidationResult withIssues(String... issues) {
    if (issues == null || issues.length == 0) {
      return new DefaultValidationResult(List.copyOf(this.issues));
    }

    List<String> updated =
        Arrays.stream(issues).filter(Objects::nonNull).collect(Collectors.toList());
    updated.addAll(List.copyOf(this.issues));

    return new DefaultValidationResult(List.copyOf(updated));
  }

  /**
   * Accumulates itself to be combined with other issues.
   *
   * @param issues additional issues to be accumulated.
   * @return a new instance of itself with the additional value.
   */
  @Override
  public DefaultValidationResult withIssues(Collection<String> issues) {
    if (issues == null || issues.isEmpty()) {
      return new DefaultValidationResult(List.copyOf(this.issues));
    }
    List<String> updated = new ArrayList<>(this.issues);
    updated.addAll(issues);
    return new DefaultValidationResult(List.copyOf(updated));
  }

  /**
   * Combines itself with other {@link ValidationResult} implementation.
   *
   * @param other other instance.
   * @return a new instance of itself as an accumulated result.
   */
  @Override
  public DefaultValidationResult combine(ValidationResult other) {
    List<String> list = new ArrayList<>(this.issues);
    list.addAll(other.getIssues());
    return new DefaultValidationResult(List.copyOf(list));
  }

  /**
   * A method to check if current result is valid (in other words, empty).
   *
   * @return true if no issues were accumulated, else returns false.
   */
  @Override
  public boolean isValid() {
    return this.issues.isEmpty();
  }

  /**
   * A method to retrieve accumulated issues as an immutable list.
   *
   * @return immutable list of the accumulated result.
   */
  @Override
  public List<String> getIssues() {
    return List.copyOf(this.issues);
  }
}
