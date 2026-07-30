package io.dsub.discogs.batch;

import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
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

  private final Job job;
  private final JobParameters discogsJobParameters;
  private final JobOperator jobOperator;
  private final ConfigurableApplicationContext ctx;
  private final CountDownLatch countDownLatch;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    JobExecution jobExecution = jobOperator.start(job, discogsJobParameters);
    log.info("main thread started job execution. awaiting for completion...");
    countDownLatch.await();
    log.info("job execution completed. exiting...");
    SpringApplication.exit(ctx, getExitCodeGenerator(jobExecution));
  }

  public ExitCodeGenerator getExitCodeGenerator(JobExecution jobExecution) {
    return () -> jobExecution.getFailureExceptions().size() > 0 ? 1 : 0;
  }
}
