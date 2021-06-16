package io.dsub.discogs.batch;

import io.dsub.discogs.batch.argument.handler.ArgumentHandler;
import io.dsub.discogs.batch.argument.handler.DefaultArgumentHandler;
import io.dsub.discogs.batch.util.JdbcConnectionTester;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class BatchService {
  protected ConfigurableApplicationContext run(String[] args) throws Exception {
    String[] resolvedArguments = getArgumentHandler().resolve(args);
    getJdbcConnectionTester().testConnection(resolvedArguments);
    return runSpringApplication(resolvedArguments);
  }
  protected ConfigurableApplicationContext runSpringApplication(String[] args) {
    return SpringApplication.run(BatchApplication.class, args);
  }
  protected JdbcConnectionTester getJdbcConnectionTester() {
    return new JdbcConnectionTester();
  }
  protected ArgumentHandler getArgumentHandler() {
    return new DefaultArgumentHandler();
  }
}