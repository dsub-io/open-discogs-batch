package io.dsub.discogsdata.batch.argument.validator;

import java.util.Collection;
import java.util.List;

/**
 * Represented result of a validation (mainly by {@link ArgumentValidator}). actual behavior depends
 * on its implementations.
 */
public interface ValidationResult {

  /**
   * Wither pattern to support adding a single issue.
   *
   * @param issue to be added.
   * @return accumulated, new instance.
   */
  ValidationResult withIssue(String issue);

  /**
   * Wither pattern to support adding multiple issues without bound.
   *
   * @param issues to be added.
   * @return accumulated, new instance.
   */
  ValidationResult withIssues(String... issues);

  /**
   * Wither pattern to support adding a collection of issues.
   *
   * @param issues to be added.
   * @return accumulated, new instance.
   */
  ValidationResult withIssues(Collection<String> issues);

  /**
   * Wither pattern to support adding another instance.
   *
   * @param other instance to be accumulated.
   * @return accumulated, new instance.
   */
  ValidationResult combine(ValidationResult other);

  /**
   * Validation check to be added. The actual behavior will be depends on the implementation.
   *
   * @return evaluation of current accumulated result.
   */
  boolean isValid();

  /**
   * A support method to fetch accumulated issues.
   *
   * @return list of issues.
   */
  List<String> getIssues();
}
