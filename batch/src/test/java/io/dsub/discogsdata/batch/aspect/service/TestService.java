package io.dsub.discogsdata.batch.aspect.service;

import org.springframework.stereotype.Service;

@Service
public class TestService {
  void someMethod(String input) {}

  void throwingMethod() throws RuntimeException {
    throw new RuntimeException("exception message");
  }
}
