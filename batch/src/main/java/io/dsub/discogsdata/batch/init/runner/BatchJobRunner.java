package io.dsub.discogsdata.batch.init.runner;

import io.dsub.discogsdata.batch.init.converter.DiscogsJobParametersConverter;
import io.dsub.discogsdata.batch.init.job.JobParameterResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class BatchJobRunner implements ApplicationRunner {

  private final DiscogsJobParametersConverter converter;
  private final JobParameterResolver jobParameterResolver;
  private final JobLauncher jobLauncher;

  @Override
  public void run(ApplicationArguments args) {
    Properties props = jobParameterResolver.resolve(args);
    JobParameters jobParameters = converter.getJobParameters(props);
    System.out.println(jobParameters);
  }
}
