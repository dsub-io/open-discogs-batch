package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.argument.handler.DefaultArgumentHandler;
import io.dsub.discogsdata.batch.util.JdbcConnectionTester;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@EnableAspectJAutoProxy
@SpringBootApplication(
    scanBasePackages = {"io.dsub.discogsdata.common", "io.dsub.discogsdata.batch"})
public class BatchApplication {
  public static void main(String[] args) {
    try {
      args = new DefaultArgumentHandler().resolve(args);
      new JdbcConnectionTester().testConnection(args);
    } catch (InvalidArgumentException e) {
      System.out.println(e.getMessage());
      System.exit(1);
    }
    SpringApplication.run(BatchApplication.class, args);
  }
}
