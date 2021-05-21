package io.dsub.discogsdata.batch.argument;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ArgumentPropertyUnitTest {

  @Test
  void builderShouldContainValidInitialValues() {
    ArgumentProperty property = ArgumentProperty.builder().globalName("testProperty").build();
    assertThat(property.getMaxValuesCount()).isEqualTo(1);
    assertThat(property.getMinValuesCount()).isEqualTo(1);
    assertThat(property.getSupportedType()).isEqualTo(String.class);
    assertThat(property.getGlobalName()).isEqualTo("testProperty");
    assertThat(property.getSynonyms().size()).isEqualTo(1);
  }

  @Test
  void builderShouldThrowWhenNullValuePassedAsGlobalName() {
    assertThrows(InvalidArgumentException.class, () -> ArgumentProperty.builder().build());
    assertThrows(
        InvalidArgumentException.class, () -> ArgumentProperty.builder().globalName(null).build());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 66, 33, 77, 22, 88})
  void builderShouldRepresentGivenValues(int min) {
    String globalName = String.valueOf(min * 3 * 3);
    List<String> synonyms =
        List.of(
            String.valueOf(min),
            String.valueOf(min * 3),
            String.valueOf(min + min * 3),
            globalName);
    boolean required = min < 33;
    Class<?> supportedType = min + min * 3 > 110 ? Long.class : Double.class;
    ArgumentProperty props =
        ArgumentProperty.builder()
            .minValuesCount(min)
            .maxValuesCount(min * 3)
            .synonyms(String.valueOf(min), String.valueOf(min * 3), String.valueOf(min + min * 3))
            .required(required)
            .supportedType(supportedType)
            .globalName(String.valueOf(min * 3 * 3))
            .build();

    for (String synonym : synonyms) {
      assertThat(synonym).isIn(props.getSynonyms());
    }
    assertThat(props.getSynonyms().size()).isEqualTo(synonyms.size());
    assertThat(props.isRequired()).isEqualTo(required);
    assertThat(props.getMaxValuesCount()).isEqualTo(min * 3);
    assertThat(props.getMinValuesCount()).isEqualTo(min);
    assertThat(props.getGlobalName()).isEqualTo(globalName);
    assertThat(props.getSupportedType()).isEqualTo(supportedType);
  }

  @Test
  void containsShouldReturnValidResultIfUnderscoreOrHyphen() {
    ArgumentProperty property =
        ArgumentProperty.builder()
            .globalName("testProperty")
            .synonyms("hello", "jackson", "odinson")
            .build();
    assertThat(property.contains("hel-lo")).isTrue();
    assertThat(property.contains("hel_lo")).isTrue();
    assertThat(property.contains("odin-son")).isTrue();
    assertThat(property.contains("odin_son")).isTrue();
    assertThat(property.contains("j-a_c_k_s-o-n")).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"jackson", "johnson", "emerson", "odinson"})
  void getGlobalNameShouldReturnValidName(String globalName) {
    ArgumentProperty property = ArgumentProperty.builder().globalName(globalName).build();
    assertThat(property.getGlobalName()).isEqualTo(globalName);
  }

  @ParameterizedTest
  @ValueSource(strings = {"orange", "tangerine", "apple"})
  void synonymsShouldContainValidValues(String synonym) {
    ArgumentProperty property =
        ArgumentProperty.builder().globalName("test").synonyms(synonym).build();
    assertThat("test").isIn(property.getSynonyms());
    assertThat(synonym).isIn(property.getSynonyms());
  }

  @Test
  void isRequired() {
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 53, 673, 12, 55, 3})
  void shouldThrowWhenMinValueIsGreaterThanMaxValue(int minValue) {
    int max = 50;
    if (minValue <= max) {
      assertDoesNotThrow(
          () ->
              ArgumentProperty.builder()
                  .globalName("test")
                  .minValuesCount(minValue)
                  .maxValuesCount(max)
                  .build());
    } else {
      assertThrows(
          InvalidArgumentException.class,
          () ->
              ArgumentProperty.builder()
                  .globalName("test")
                  .minValuesCount(minValue)
                  .maxValuesCount(max)
                  .build());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"the", "finest", "wines", "and", "beers"})
  void shouldProperlyBuildPropertyWithGivenSynonyms(String word) {
    List<String> list = List.of(word);
    ArgumentProperty property =
        ArgumentProperty.builder().globalName("test").synonyms(list).build();
    assertThat(word).isIn(property.getSynonyms());
    assertThat("test").isIn(property.getSynonyms());
  }

  @ParameterizedTest
  @ValueSource(strings = {"hello", "world", "something"})
  void shouldHandleNullSynonymsPassedToBuilder(String name) {
    List<String> nullList = null;
    String nullString = null;
    assertDoesNotThrow(
        () -> ArgumentProperty.builder().globalName(name).synonyms(nullList).build());
    assertDoesNotThrow(
        () -> ArgumentProperty.builder().globalName(name).synonyms(nullString).build());
  }

  @Test
  void shouldHandleNullInvolvedListOrArrayIntoSynonyms() {
    List<String> list =
        List.of("hi", "there").stream()
            .map(s -> s.equals("hi") ? null : s)
            .collect(Collectors.toList());
    ArgumentProperty property =
        ArgumentProperty.builder().globalName("test").synonyms(list).build();
    assertThat(property.getSynonyms().size()).isEqualTo(2);

    property =
        ArgumentProperty.builder().globalName("test").synonyms(list.toArray(String[]::new)).build();
    assertThat(property.getSynonyms().size()).isEqualTo(2);
  }

  @Test
  void shouldThrowWhenMinValueIsSmallerThanZero() {
    int min = -1;
    assertThrows(
        InvalidArgumentException.class,
        () ->
            ArgumentProperty.builder()
                .globalName("test")
                .minValuesCount(min)
                .maxValuesCount(10)
                .build());
  }

  @ParameterizedTest
  @ValueSource(classes = {String.class, Long.class, Double.class})
  void getSupportedType(Class<?> type) {
    ArgumentProperty property =
        ArgumentProperty.builder().globalName("test").supportedType(type).build();
    assertThat(property.getSupportedType()).isEqualTo(type);
  }
}
