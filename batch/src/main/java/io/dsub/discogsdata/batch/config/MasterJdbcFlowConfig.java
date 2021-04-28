package io.dsub.discogsdata.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MasterJdbcFlowConfig {

    private final StepBuilderFactory sbf;

    @Bean
    @JobScope
    public Step masterStep(@Value("#{jobParameters['master']}") String etag) {
        return sbf.get("masterStep " + etag)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
                .build();
    }
}
