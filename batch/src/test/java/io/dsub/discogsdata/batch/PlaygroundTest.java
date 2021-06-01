package io.dsub.discogsdata.batch;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PlaygroundTest {

  @Test
  void test() {
    List<String> strings = new ArrayList<>();

    System.out.println(strings.getClass().getGenericSuperclass());
    System.out.println(strings.getClass().getTypeName());
  }
}
