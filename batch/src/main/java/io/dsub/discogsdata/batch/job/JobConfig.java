package io.dsub.discogsdata.batch.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JobConfig {
    private static final String STEP_NAME = "myStep";
    private final JobBuilderFactory jbf;
    private final StepBuilderFactory sbf;
}
