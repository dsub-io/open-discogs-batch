package io.dsub.discogs.batch;

import io.dsub.discogs.batch.util.JdbcConnectionTester;
import io.dsub.discogs.batch.argument.handler.DefaultArgumentHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication(scanBasePackages = {"io.dsub.discogs.common", "io.dsub.discogs.batch"})
public class BatchApplication {

  public static void main(String[] args) {
    try {
      args = new DefaultArgumentHandler().resolve(args);
      new JdbcConnectionTester().testConnection(args);
      ApplicationContext context = SpringApplication.run(BatchApplication.class, args);
      System.exit(SpringApplication.exit(context));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }
}