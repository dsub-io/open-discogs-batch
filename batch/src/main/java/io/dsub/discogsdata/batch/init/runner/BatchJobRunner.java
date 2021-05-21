package io.dsub.discogsdata.batch.init.runner;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.batch.dump.service.DiscogsDumpService;
import io.dsub.discogsdata.batch.init.converter.DiscogsJobParametersConverter;
import io.dsub.discogsdata.batch.init.job.JobParameterResolver;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class BatchJobRunner implements ApplicationRunner {

  private final DiscogsJobParametersConverter converter;
  private final JobParameterResolver jobParameterResolver;
  private final JobLauncher jobLauncher;
  private final DiscogsDumpService dumpService;

  @Override
  public void run(ApplicationArguments args) {
    Properties props = jobParameterResolver.resolve(args);

    String artistETag = props.getProperty(DumpType.ARTIST.toString());
    DiscogsDump artistDump = dumpService.getDiscogsDump(artistETag);

    JobParameters jobParameters = converter.getJobParameters(props);
  }
}
