package io.dsub.discogsdata.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
  private final JobLauncher jobLauncher;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    jobLauncher.run(job, discogsJobParameters);
  }
}
