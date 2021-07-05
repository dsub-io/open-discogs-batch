package io.dsub.discogs.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@Slf4j
@SpringBootApplication(exclude = {HibernateJpaAutoConfiguration.class})
public class BatchApplication {

  private static ConfigurableApplicationContext APP_CONTEXT;

  public static void main(String[] args) {
    BatchService service = getBatchService();
    try {
      APP_CONTEXT = service.run(args);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  protected static BatchService getBatchService() {
    return new BatchService();
  }

  @Bean(name = "parentContext")
  public ConfigurableApplicationContext parentContext() {
    return APP_CONTEXT;
  }
}
