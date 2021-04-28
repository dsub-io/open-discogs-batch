package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.common.exception.MissingRequiredParamsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultQueryStepProvider implements QueryStepProvider {

    private final StepBuilderFactory factory;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Step get(String stepName, String... queries) {
        int queryCount = queries.length;

        if (queryCount == 0) {
            throw new MissingRequiredParamsException("expected at least one query");
        }

        return factory.get(stepName)
                .tasklet(getQueryingTasklet(queries))
                .allowStartIfComplete(true)
                .build();
    }

    @Override
    public Tasklet getQueryingTasklet(String[] queries) {
        return (contribution, chunkContext) -> {
            for (String query : queries) {
                try {
                    jdbcTemplate.execute(query);
                } catch (Exception e) {
                    DefaultQueryStepProvider.log.error(e.getMessage());
                    contribution.setExitStatus(ExitStatus.FAILED);
                    chunkContext.setComplete();
                    return RepeatStatus.FINISHED;
                }
            }
            contribution.setExitStatus(ExitStatus.COMPLETED);
            chunkContext.setComplete();
            return RepeatStatus.FINISHED;
        };
    }
}
