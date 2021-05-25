package io.dsub.discogsdata.batch.job;

import io.dsub.discogsdata.batch.job.listener.SystemExitingJobExecutionListener;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
// TODO: write integration test
public class DefaultJobCreator implements JobCreator {

  private static final String FAILED = "FAILED";
  private static final String ANY = "*";

  private final JobBuilderFactory jobBuilderFactory;
  private final Step artistStep;

  @Override
  public Job createJob(JobParameters jobParameters) {
    LocalDateTime ldt = LocalDateTime.now(Clock.systemUTC());
    return jobBuilderFactory
        .get("discogs-batch-job " + ldt)
        .start(artistStep)
        .on(ANY)
        .end()
        .build()
        .listener(new SystemExitingJobExecutionListener())
        .build();
  }
}
