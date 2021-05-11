package io.dsub.discogsdata.batch.init.runner;

import io.dsub.discogsdata.batch.dump.service.DiscogsDumpService;
import io.dsub.discogsdata.batch.init.converter.DiscogsJobParametersConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class BatchJobRunner implements ApplicationRunner {

  private final Environment environment;
  private final DiscogsJobParametersConverter converter;
  private final DiscogsDumpService dumpService;
  private final JobLauncher jobLauncher;
  //    private final JobCreator jobCreator;
  //    private final SimpleDiscogsBatchJobParameterResolver
  //   DiscogsJobParameterConverter;

  @Override
  public void run(ApplicationArguments args) {
    System.out.println(getClass().getSimpleName() + " running!");
    JobParameters params = makeJobParameters(args);
    System.out.println(params);
    //        jobLauncher.run(jobCreator.make(params), params);
  }

  private JobParameters makeJobParameters(ApplicationArguments args) {
    return converter.getJobParameters(args);
  }
}
