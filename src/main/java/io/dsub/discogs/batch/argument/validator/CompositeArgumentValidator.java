package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.exception.MissingRequiredParamsException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;

/**
 * Composite argument validator that delegates its validation to list of other validators. Can be
 * used to tread a group of validators as a single validator. Also, a group of validators can also
 * be accumulated accordingly.
 */
public class CompositeArgumentValidator implements ArgumentValidator, InitializingBean {

  /** A list of delegates that will actually perform. */
  private final List<ArgumentValidator> delegates = new ArrayList<>();

  /**
   * Adds validator to its delegates list.
   *
   * @param delegate a validator to delegate.
   * @return itself.
   */
  public CompositeArgumentValidator addValidator(ArgumentValidator delegate) {
    if (delegate == null) {
      return this;
    }
    this.delegates.add(delegate);
    return this;
  }

  /**
   * Adds list of validators to its delegates list.
   *
   * @param delegates additional list of delegates to be added.
   * @return itself.
   */
  public CompositeArgumentValidator addValidators(List<ArgumentValidator> delegates) {
    if (delegates == null) {
      return this;
    }
    this.delegates.addAll(delegates);
    return this;
  }

  /**
   * Adds unbounded numbers of validators to its delegates list.
   *
   * @param delegates additional validators to be added (or can be null)
   * @return itself.
   */
  public CompositeArgumentValidator addValidators(ArgumentValidator... delegates) {
    if (delegates == null || delegates.length == 0) {
      return this;
    }
    this.delegates.addAll(List.of(delegates));
    return this;
  }

  /**
   * Method to delegate validations.
   *
   * @param applicationArguments to be validated.
   * @return result of validation set accumulated by validators.
   */
  @Override
  public ValidationResult validate(ApplicationArguments applicationArguments) {
    if (applicationArguments == null) {
      return new DefaultValidationResult("applicationArgument cannot be null");
    }
    ValidationResult result = new DefaultValidationResult();
    for (ArgumentValidator delegate : delegates) {
      result = result.combine(delegate.validate(applicationArguments));
    }
    return result;
  }

  /**
   * Method to fulfill {@link InitializingBean}.
   *
   * @throws MissingRequiredParamsException thrown if delegates are empty (cannot be null.)
   */
  @Override
  public void afterPropertiesSet() throws MissingRequiredParamsException {
    if (this.delegates.isEmpty()) {
      throw new MissingRequiredParamsException("delegates must not be null or empty");
    }
  }
}
