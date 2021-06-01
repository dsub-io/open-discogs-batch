package io.dsub.discogsdata.batch.aspect.service;

import java.lang.reflect.Field;
import org.springframework.stereotype.Service;

@Service
public class TestService {

  public void someMethod(String input) {
  }

  public void throwingMethod() throws RuntimeException {
    throw new RuntimeException("exception message");
  }

  public void methodTakeField(Field field) {
  }

  public void methodTakeClass(Class<?> clazz) {
  }
}
