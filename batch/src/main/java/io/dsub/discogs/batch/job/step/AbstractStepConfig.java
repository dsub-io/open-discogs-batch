package io.dsub.discogs.batch.job.step;

import io.dsub.discogs.batch.dump.DumpType;
import io.dsub.discogs.common.exception.InvalidArgumentException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
public abstract class AbstractStepConfig {

  protected static final String CHUNK = "#{jobParameters['chunkSize']}";
  protected static final String THROTTLE = "#{jobParameters['throttleLimit']}";
  protected static final String ANY = "*";
  protected static final String FAILED = "FAILED";
  protected static final String SKIPPED = "SKIPPED";

  protected <T> ItemWriter<T> buildItemWriter(String query, DataSource dataSource) {
    if (query == null || query.isBlank()) {
      throw new InvalidArgumentException("query cannot be null or blank.");
    }
    if (dataSource == null) {
      throw new InvalidArgumentException("datasource cannot be null.");
    }
    return new JdbcBatchItemWriterBuilder<T>()
        .sql(query)
        .dataSource(dataSource)
        .beanMapped()
        .assertUpdates(false)
        .build();
  }

  protected JobExecutionDecider getOnKeyExecutionDecider(String key) {
    if (key == null || key.isBlank()) {
      throw new InvalidArgumentException("key cannot be null or blank.");
    }
    return (jobExecution, stepExecution) -> {
      if (jobExecution.getJobParameters().getParameters().containsKey(key)) {
        log.debug(key + " eTag found. executing " + key + " step.");
        return FlowExecutionStatus.COMPLETED;
      }
      log.debug(key + " eTag not found. skipping " + key + " step.");
      return new FlowExecutionStatus(SKIPPED);
    };
  }

  protected Step buildSkipStep(DumpType dumpType, StepBuilderFactory factory) {
    return factory.get("skip-step-" + dumpType.toString())
        .tasklet((contribution, chunkContext) -> {
          log.info("skipping {} step", dumpType);
          contribution.setExitStatus(ExitStatus.COMPLETED);
          chunkContext.setComplete();
          return RepeatStatus.FINISHED;
        })
        .allowStartIfComplete(true)
        .build();
  }
}
