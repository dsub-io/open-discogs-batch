package io.dsub.discogs.batch;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.job.JobParameterResolver;

import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.utils.InputStreamReader;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Order(0)
@Configuration
@RequiredArgsConstructor
public class JobPreparationRunner implements ApplicationRunner {

  private final JobParameterResolver jobParameterResolver;
  private final JobParametersConverter jobParametersConverter;
  private final DataSource dataSource;
  private final ThreadPoolTaskExecutor taskExecutor;

  @Override
  public void run(ApplicationArguments args) {
    int poolSize = taskExecutor.getMaxPoolSize() + 3;

    if (dataSource instanceof HikariDataSource) {
      log.info("setting db connection pool size to " + poolSize);
      ((HikariDataSource) dataSource).setMaximumPoolSize(poolSize);
    }
  }

  @Bean
  public CountDownLatch exitLatch() {
    return new CountDownLatch(1);
  }

  @Bean(name = "discogsJobParameters")
  public JobParameters getDiscogsJobParameters(ApplicationArguments args)
      throws InvalidArgumentException, DumpNotFoundException {
    log.info("resolving given job parameters");
    Properties props = jobParameterResolver.resolve(args);
    return jobParametersConverter.getJobParameters(props);
  }
}
