package io.dsub.discogsdata.batch.argument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArgTypeUnitTest {

  @ParameterizedTest
  @ValueSource(strings = {"chunk", "e", "y", "c", "t", "throttle"})
  void shouldReturnValidBooleanForContainsMethod(String value) {
    assertThat(ArgType.contains(value)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void eachRegisteredSynonymShouldMatchWhenGetTypeOfMethodCalled(ArgType argType) {
    for (String synonym : argType.getSynonyms()) {
      assertThat(ArgType.getTypeOf(synonym)).isEqualTo(argType);
    }
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void callingGetGlobalNameShouldAlwaysReturnAMeaningfulValue(ArgType argType) {
    assertThat(argType.getGlobalName()).isNotNull().isNotBlank();
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void callingIsRequiredShouldReturnBooleanValue(ArgType argType) {
    assertThat(argType.isRequired()).isNotNull();
  }

  @Test
  void callingGetTypeOfWithNullOrBlankStringShouldReturnNull() {
    assertThat(ArgType.getTypeOf(null)).isNull();
    assertThat(ArgType.getTypeOf("")).isNull();
  }

  @Test
  void shouldReturnNullIfNoRegisteredTypeCalled() {
    assertThat(ArgType.getTypeOf("hello")).isNull();
    assertThat(ArgType.getTypeOf("world")).isNull();
    assertThat(ArgType.getTypeOf("etagss")).isNull();
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void getSynonymsShouldReturnUnmodifiableList(ArgType argType) {
    assertThrows(Exception.class, () -> argType.getSynonyms().add("hello world"));
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void getSynonymsShouldNotReturnEmptyList(ArgType argType) {
    List<String> synonyms = argType.getSynonyms();
    assertThat(synonyms).isNotNull();
    assertThat(synonyms.size()).isGreaterThan(0);
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void globalNameShouldReturnNotNullNotBlankString(ArgType argType) {
    assertThat(argType.getGlobalName()).isNotNull().isNotBlank();
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void valuesShouldReturnNotNullNotBlankArgTypes(ArgType argType) {
    assertThat(argType).isNotNull();
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void getMaxValuesCountShouldNotReturnNegativeValue(ArgType argType) {
    assertThat(argType.getMaxValuesCount()).isGreaterThan(-1);
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void getMinValuesCountShouldNotReturnNegativeValue(ArgType argType) {
    assertThat(argType.getMinValuesCount()).isGreaterThan(-1);
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void getMinValuesCountShouldNotBeGreaterThanMaxValuesCount(ArgType argType) {
    assertThat(argType.getMaxValuesCount()).isGreaterThanOrEqualTo(argType.getMinValuesCount());
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void requiredArgsMustShowMinValuesAtLeastOne(ArgType argType) {
    if (argType.isRequired()) {
      assertThat(argType.getMinValuesCount()).isGreaterThan(0);
    }
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void ifGetMinValuesCountIsZero_ThenMaxValuesCountShouldBeZero(ArgType argType) {
    if (argType.getMinValuesCount() == 0) {
      assertThat(argType.getMaxValuesCount()).isEqualTo(0);
    }
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void testValueOfReturnsProperMatch(ArgType argType) {
    String name =
        IntStream.range(0, argType.getGlobalName().length())
            .map(i -> argType.getGlobalName().charAt(i))
            .mapToObj(i -> String.valueOf((char) i))
            .map(s -> s.matches("[A-Z]") ? "_" + s : s)
            .map(String::toUpperCase)
            .collect(Collectors.joining(""));
    assertThat(ArgType.valueOf(name)).isNotNull();
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void isValueRequiredShouldReturnProperly(ArgType argType) {
    int min = argType.getMinValuesCount();
    assertThat(argType.isValueRequired()).isEqualTo(min > 0);
  }

  @ParameterizedTest
  @EnumSource(ArgType.class)
  void getSupportedTypeShouldNotReturnNull(ArgType argType) {
    assertThat(argType.getSupportedType()).isNotNull();
  }
}
