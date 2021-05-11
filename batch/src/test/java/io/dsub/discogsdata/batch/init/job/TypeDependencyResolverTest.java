package io.dsub.discogsdata.batch.init.job;

import io.dsub.discogsdata.batch.dump.DumpType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TypeDependencyResolverTest {
  final TypeDependencyResolver resolver = new TypeDependencyResolver();

  @ParameterizedTest
  @ValueSource(strings = {"release", "artist", "master", "label"})
  void whenGetDependantTypesCalled__ShouldReturnEachDependingItems(String typeName) {

    int expectedSize = 1;
    if (typeName.equals("release")) {
      expectedSize = 4;
    } else if (typeName.equals("master")) {
      expectedSize = 3;
    }

    // when
    List<String> resultList = resolver.getDependantTypes(DumpType.of(typeName));

    // then
    assertThat(resultList.size()).isEqualTo(expectedSize);
    assertThat(typeName).isIn(resultList);
  }

  @Test
  void whenNoTypesSpecified__ThenShouldReturnAllTypes() {
    // when
    List<String> resultList = resolver.resolveType(new DefaultApplicationArguments());

    // then
    assertThat(resultList.size()).isEqualTo(4);
    for (DumpType dumpType : DumpType.values()) {
      assertThat(dumpType.toString()).isIn(resultList);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"release", "artist", "master", "label"})
  void whenAnyTypeSpecified__ThenShouldReturnAccordingly(String typeName) {
    String prefix = "--type=";

    int expectedSize = 1;
    if (typeName.equals("release")) {
      expectedSize = 4;
    } else if (typeName.equals("master")) {
      expectedSize = 3;
    }

    ApplicationArguments args = new DefaultApplicationArguments(prefix + typeName);

    // when
    List<String> resultList = resolver.resolveType(args);

    // then
    assertThat(resultList.size()).isEqualTo(expectedSize);
    assertThat(typeName).isIn(resultList);
  }
}
