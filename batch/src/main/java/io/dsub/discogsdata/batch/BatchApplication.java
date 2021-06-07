package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.argument.handler.DefaultArgumentHandler;
import io.dsub.discogsdata.batch.util.JdbcConnectionTester;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication(scanBasePackages = {"io.dsub.discogsdata.common",
    "io.dsub.discogsdata.batch"})
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