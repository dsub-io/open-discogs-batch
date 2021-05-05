package io.dsub.discogsdata.batch.argument.validator;

import org.springframework.boot.ApplicationArguments;

/**
 * an interface representing a validator for {@link ApplicationArguments}.
 * actual validation process are dependent on its implementation.
 */
public interface ArgumentValidator {
    /**
     * method to validate {@link ApplicationArguments}.
     *
     * @param applicationArguments to be validated.
     * @return result of validation represented as {@link ValidationResult}.
     */
    ValidationResult validate(ApplicationArguments applicationArguments);
}