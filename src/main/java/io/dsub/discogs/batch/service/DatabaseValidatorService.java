package io.dsub.discogs.batch.service;

import io.dsub.discogs.batch.argument.validator.ValidationResult;
import org.springframework.boot.ApplicationArguments;

public interface DatabaseValidatorService {
    /**
     * validates database from url, username and password.
     * @param url a jdbc url
     * @param username a username
     * @param password a password
     * @return blank result if no issues found, otherwise will contain one or many issues.
     */
    ValidationResult validate(String url, String username, String password);


    /**
     * validates database from url, username and password.
     * @param args a {@link ApplicationArguments} that contains url, username, password as option argument.
     * @return blank result if no issues found, otherwise will contain one or many issues.
     */
    ValidationResult validate(ApplicationArguments args);
}