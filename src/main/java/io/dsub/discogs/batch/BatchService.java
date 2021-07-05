package io.dsub.discogs.batch;

import io.dsub.discogs.batch.argument.handler.ArgumentHandler;
import io.dsub.discogs.batch.argument.handler.DefaultArgumentHandler;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class BatchService {

  protected ConfigurableApplicationContext run(String[] args) throws Exception {
    String[] resolved = resolveArguments(args);
    if (resolved == null) {
      log.info("Exiting...");
      System.exit(1);
    }
    return runSpringApplication(resolveArguments(args));
  }

  protected ConfigurableApplicationContext runSpringApplication(String[] args) {
    return SpringApplication.run(BatchApplication.class, args);
  }

  protected String[] resolveArguments(String[] args) {
    try {
      return getArgumentHandler().resolve(args);
    } catch (Exception e) {
      return null;
    }
  }

  protected ArgumentHandler getArgumentHandler() {
    return new DefaultArgumentHandler();
  }
}