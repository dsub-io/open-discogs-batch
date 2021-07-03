package io.dsub.discogs.batch;

import io.dsub.discogs.batch.argument.handler.ArgumentHandler;
import io.dsub.discogs.batch.argument.handler.DefaultArgumentHandler;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.service.DatabaseValidatorService;
import io.dsub.discogs.batch.service.DefaultDatabaseValidatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class BatchService {
    protected ConfigurableApplicationContext run(String[] args) throws Exception {
        String[] resolvedArguments = getArgumentHandler().resolve(args);
        ValidationResult result = getDatabaseValidation(resolvedArguments);

        if (!result.isValid()) {
            result.getIssues().forEach(log::error);
            return null;
        }
        return runSpringApplication(resolvedArguments);
    }

    private ValidationResult getDatabaseValidation(String[] resolvedArguments) {
        return getDatabaseValidatorService().validate(new DefaultApplicationArguments(resolvedArguments));
    }

    protected ConfigurableApplicationContext runSpringApplication(String[] args) {
        return SpringApplication.run(BatchApplication.class, args);
    }

    protected DatabaseValidatorService getDatabaseValidatorService() {
        return new DefaultDatabaseValidatorService();
    }

    protected ArgumentHandler getArgumentHandler() {
        return new DefaultArgumentHandler();
    }
}