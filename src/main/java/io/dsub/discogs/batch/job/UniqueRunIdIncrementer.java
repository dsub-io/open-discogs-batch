package io.dsub.discogs.batch.job;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;

public class UniqueRunIdIncrementer extends RunIdIncrementer {
  private static final String RUN_ID = "run.id";

  @Override
  public JobParameters getNext(JobParameters parameters) {
    JobParameters params = (parameters == null) ? new JobParameters() : parameters;
    Long lastVal = params.getLong(RUN_ID, Long.valueOf(1));
    lastVal = lastVal == null ? 1L : lastVal;
    return new JobParametersBuilder()
        .addLong(RUN_ID, lastVal + 1)
        .toJobParameters();
  }
}
