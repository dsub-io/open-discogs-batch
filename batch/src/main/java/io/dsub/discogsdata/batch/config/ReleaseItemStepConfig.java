package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.dump.DumpService;
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
public class ReleaseItemStepConfig {

    private final DumpService dumpService;
    private final StepBuilderFactory sbf;

//    private final Step releaseItemRelationJdbcTempTableGenerationStep;
//    private final Step releaseRelationJdbcTempTableDropStep;
//    private final Step releaseRelationJdbcPruneStep;
//    private final Step releaseRelationJdbcCopyStep;


    @Bean
    @JobScope
    public Step releaseItemStep(@Value("#{jobParameters['release']}") String etag) {
        return sbf.get("releaseItemStep " + etag)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED)
                .build();
    }
}
