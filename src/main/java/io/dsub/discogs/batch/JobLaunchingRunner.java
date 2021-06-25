package io.dsub.discogs.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Slf4j
@Order(10)
@Profile("!test")
@Component
@RequiredArgsConstructor
public class JobLaunchingRunner implements ApplicationRunner {

    private final Job job;
    private final JobParameters discogsJobParameters;
    private final JobLauncher jobLauncher;
    private final ConfigurableApplicationContext ctx;
    private final CountDownLatch countDownLatch;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        JobExecution jobExecution = jobLauncher.run(job, discogsJobParameters);
        log.info("main thread started job execution. awaiting for completion...");

        countDownLatch.await();

        log.info("job execution completed. dropping temporary tables...");
        SpringApplication.exit(ctx, getExitCodeGenerator(jobExecution));
    }

    public ExitCodeGenerator getExitCodeGenerator(JobExecution jobExecution) {
        return () -> jobExecution.getFailureExceptions().size() > 0 ? 1 : 0;
    }
}