package io.dsub.discogs.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication(scanBasePackages = {"io.dsub.discogs.common", "io.dsub.discogs.batch"})
public class BatchApplication {

  public static void main(String[] args) {
    BatchService service = getBatchService();
    try {
      service.run(args);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  protected static BatchService getBatchService() {
    return new BatchService();
  }
}