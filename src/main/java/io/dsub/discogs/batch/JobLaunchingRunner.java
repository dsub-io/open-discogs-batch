package io.dsub.discogs.batch;

import io.dsub.discogs.batch.job.DownloadedFileCleanup;
import io.dsub.discogs.batch.job.ImportExecutionCoordinator;
import io.dsub.discogs.batch.job.ImportJobParameters;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(10)
@Profile("!test")
@Component
@RequiredArgsConstructor
public class JobLaunchingRunner implements ApplicationRunner {

  private static final String FAILED_EXIT_CODE = "FAILED";
  private static final String BATCH_JOB_FAILED_MESSAGE = "batch job failed";

  private final Job job;
  private final JobParameters discogsJobParameters;
  private final JobOperator jobOperator;
  private final ConfigurableApplicationContext ctx;
  private final CountDownLatch countDownLatch;
  private final ImportExecutionCoordinator importExecutionCoordinator;
  private final DownloadedFileCleanup downloadedFileCleanup;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    ImportExecutionCoordinator.Preparation preparation =
        importExecutionCoordinator.prepare(discogsJobParameters);
    if (preparation.skipped()) {
      log.info(
          "import skipped because manifest {} already succeeded as run {}",
          preparation.manifestSha256(),
          preparation.priorSuccessfulRunId());
      if (downloadedFileCleanup.isEnabled()) {
        downloadedFileCleanup.cleanup(discogsJobParameters);
      }
      SpringApplication.exit(ctx, () -> 0);
      return;
    }

    JobExecution jobExecution = null;
    boolean coordinatorCompleted = false;
    try {
      JobParameters executionParameters =
          new JobParametersBuilder(discogsJobParameters)
              .addLong(ImportJobParameters.RUN_ID, preparation.runId())
              .addString(
                  ImportJobParameters.RESUMED,
                  Boolean.toString(preparation.resumedFromRunId() != null))
              .toJobParameters();
      jobExecution = jobOperator.start(job, executionParameters);
      log.info("main thread started job execution. awaiting for completion...");
      countDownLatch.await();
      log.info("job execution completed. exiting...");
      boolean success = isSuccessful(jobExecution);
      Throwable failure = failureOf(jobExecution, success);
      importExecutionCoordinator.complete(success, failure);
      coordinatorCompleted = true;
      if (success && downloadedFileCleanup.isEnabled()) {
        downloadedFileCleanup.cleanup(discogsJobParameters);
      }
      SpringApplication.exit(ctx, getExitCodeGenerator(jobExecution));
      if (!success) {
        throw new IllegalStateException(BATCH_JOB_FAILED_MESSAGE, failure);
      }
    } catch (Exception exception) {
      if (!coordinatorCompleted) {
        importExecutionCoordinator.complete(false, exception);
      }
      throw exception;
    }
  }

  public ExitCodeGenerator getExitCodeGenerator(JobExecution jobExecution) {
    return () -> isSuccessful(jobExecution) ? 0 : 1;
  }

  private boolean isSuccessful(JobExecution jobExecution) {
    return jobExecution.getStatus() == BatchStatus.COMPLETED
        && !FAILED_EXIT_CODE.equals(jobExecution.getExitStatus().getExitCode())
        && jobExecution.getFailureExceptions().isEmpty();
  }

  private Throwable failureOf(JobExecution jobExecution, boolean success) {
    if (success) {
      return null;
    }
    if (!jobExecution.getFailureExceptions().isEmpty()) {
      return jobExecution.getFailureExceptions().getFirst();
    }
    return new IllegalStateException(
        BATCH_JOB_FAILED_MESSAGE
            + ": status="
            + jobExecution.getStatus()
            + ", exit="
            + jobExecution.getExitStatus());
  }
}
