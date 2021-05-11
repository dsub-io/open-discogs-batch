package io.dsub.discogsdata.batch.init.runner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(0)
@Component
@RequiredArgsConstructor
public class JobArgumentResolvingRunner implements ApplicationRunner {
  @Override
  public void run(ApplicationArguments args) throws Exception {
  }
}
