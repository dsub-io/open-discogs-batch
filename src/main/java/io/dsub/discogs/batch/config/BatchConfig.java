package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.job.listener.ClearanceJobExecutionListener;
import io.dsub.discogs.batch.job.listener.ExitSignalJobExecutionListener;
import io.dsub.discogs.batch.job.listener.IdCachingJobExecutionListener;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

  public static final int DEFAULT_CHUNK_SIZE = 500;

  public static final String JOB_NAME = "discogs-batch-job" + LocalDateTime.now();
  private static final String FAILED = "FAILED";
  private static final String ANY = "*";

  private final Step artistStep;
  private final Step labelStep;
  private final Step masterStep;
  private final Step releaseStep;

  private final JobRepository jobRepository;
  private final IdCachingJobExecutionListener idCachingJobExecutionListener;
  private final ExitSignalJobExecutionListener exitSignalJobExecutionListener;
  private final ClearanceJobExecutionListener clearanceJobExecutionListener;

  @Bean
  public Job discogsBatchJob() {
    // @formatter:off
    return new JobBuilder(JOB_NAME, jobRepository)

        // listeners
        .listener(idCachingJobExecutionListener)
        .listener(exitSignalJobExecutionListener)
        .listener(clearanceJobExecutionListener)

        // from artist step
        .start(artistStep)
        .on(FAILED)
        .end()
        .from(artistStep)
        .on(ANY)
        .to(labelStep)

        // from label step
        .from(labelStep)
        .on(FAILED)
        .end()
        .from(labelStep)
        .on(ANY)
        .to(masterStep)

        // from master step
        .from(masterStep)
        .on(FAILED)
        .end()
        .from(masterStep)
        .on(ANY)
        .to(releaseStep)

        // from release item step
        .from(releaseStep)
        .on(ANY)
        .end()

        // build to conclude step flow
        .build()

        // build for job itself
        .build();
    // @formatter:on
  }
}
