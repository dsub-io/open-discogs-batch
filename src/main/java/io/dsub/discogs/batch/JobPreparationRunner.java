package io.dsub.discogs.batch;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.job.JobParameterResolver;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Order(0)
@Configuration
@RequiredArgsConstructor
public class JobPreparationRunner implements ApplicationRunner {

  private final JobParameterResolver jobParameterResolver;
  private final JobParametersConverter jobParametersConverter;
  private final HikariDataSource dataSource;
  private final ThreadPoolTaskExecutor taskExecutor;

  @Override
  public void run(ApplicationArguments args) {
    dataSource.setMaximumPoolSize(taskExecutor.getMaxPoolSize() + 3);
  }

  @Bean(name = "discogsJobParameters")
  public JobParameters getDiscogsJobParameters(ApplicationArguments args) {
    Properties props = jobParameterResolver.resolve(args);
    return jobParametersConverter.getJobParameters(props);
  }
}