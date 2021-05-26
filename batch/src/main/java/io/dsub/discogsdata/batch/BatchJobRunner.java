package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.job.JobCreator;
import io.dsub.discogsdata.batch.job.JobParameterResolver;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobRunner implements ApplicationRunner {

  private final JobParameterResolver jobParameterResolver;
  private final JobParametersConverter jobParametersConverter;
  private final JobLauncher jobLauncher;
  private final JobCreator jobCreator;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    Properties props = jobParameterResolver.resolve(args);
    JobParameters params = jobParametersConverter.getJobParameters(props);
    Job job = jobCreator.createJob(params);
    jobLauncher.run(job, params);
  }
}