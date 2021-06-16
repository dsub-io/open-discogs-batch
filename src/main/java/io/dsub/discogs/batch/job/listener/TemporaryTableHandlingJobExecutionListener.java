package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.datasource.DataSourceProperties;
import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryTableHandlingJobExecutionListener implements JobExecutionListener {

  private final DataSourceProperties dataSourceProperties;
  private final JdbcTemplate jdbcTemplate;
  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  @Override
  public void beforeJob(JobExecution jobExecution) {
    Resource schema = getInitSchemaResource();
    try {
      executeDDL(schema, jdbcTemplate);
    } catch (InitializationFailureException e) {
      log.error("failed to initialize temporary schema", e);
      jobExecution.setStatus(BatchStatus.FAILED);
    }
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    Resource schema = getDropSchemaResource();
    try {
      executeDDL(schema, jdbcTemplate);
    } catch (InitializationFailureException e) {
      log.error("failed to clear temporary schema", e);
      jobExecution.setStatus(BatchStatus.FAILED);
    }
  }

  private Resource getDropSchemaResource() {
    String type = dataSourceProperties.getDbType().name().toLowerCase();
    return resourceLoader.getResource("classpath:schema/" + type + "-tmp-tbl-drop.sql");
  }

  private Resource getInitSchemaResource() {
    String type = dataSourceProperties.getDbType().name().toLowerCase();
    return resourceLoader.getResource("classpath:schema/" + type + "-tmp-tbl.sql");
  }

  private void executeDDL(Resource resource, JdbcTemplate jdbcTemplate)
      throws InitializationFailureException {
    try (InputStream inputStream = resource.getInputStream()) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

      String query =
          reader.lines().filter(line -> !line.isBlank()).collect(Collectors.joining("\n"));

      Arrays.stream(query.split(";"))
          .map(String::trim)
          .collect(Collectors.toList())
          .forEach(jdbcTemplate::execute);

    } catch (IOException e) {
      throw new InitializationFailureException(e.getMessage());
    }
  }
}
