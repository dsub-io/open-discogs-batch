package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.exception.InitializationFailureException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StrictJsonObjectUnitTest {

  private static final String RESOURCE = "contract.json";

  @Test
  void readsEverySupportedJsonValueThroughTypedAccessors() {
    StrictJsonObject object =
        StrictJsonObject.parse(
            """
            {"name":"value","optional":"present","number":3,"object":{"child":"x"},"objects":[{"id":"a"},{"id":"b"}],"strings":["a","b"]}
            """,
            RESOURCE);

    object.requireFields(
        Set.of("name", "number", "object", "objects", "strings"), Set.of("optional"));
    assertThat(object.string("name")).isEqualTo("value");
    assertThat(object.optionalString("optional")).isEqualTo("present");
    assertThat(object.optionalString("absent")).isNull();
    assertThat(object.integer("number")).isEqualTo(3);
    assertThat(object.object("object").string("child")).isEqualTo("x");
    assertThat(object.objects("objects")).extracting(value -> value.string("id"))
        .containsExactly("a", "b");
    assertThat(object.strings("strings")).containsExactly("a", "b");
  }

  @Test
  void rejectsMissingUnknownAndDuplicateFields() {
    StrictJsonObject known = StrictJsonObject.parse("{\"known\":\"x\"}", RESOURCE);
    assertThatThrownBy(() -> known.requireFields(Set.of("known", "missing"), Set.of()))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("missing fields [missing]");

    StrictJsonObject unknown =
        StrictJsonObject.parse("{\"known\":\"x\",\"extra\":\"y\"}", RESOURCE);
    assertThatThrownBy(() -> unknown.requireFields(Set.of("known"), Set.of()))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("unknown fields [extra]");

    assertThatThrownBy(
            () -> StrictJsonObject.parse("{\"same\":\"x\",\"same\":\"y\"}", RESOURCE))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("duplicate field same");
  }

  @ParameterizedTest
  @MethodSource("invalidRoots")
  void rejectsNonObjectAndMalformedRoots(String json) {
    assertThatThrownBy(() -> StrictJsonObject.parse(json, RESOURCE))
        .isInstanceOf(InitializationFailureException.class);
  }

  private static Stream<String> invalidRoots() {
    return Stream.of("", "[]", "\"text\"", "{");
  }

  @ParameterizedTest
  @MethodSource("invalidStringValues")
  void rejectsValuesThatAreNotDoubleQuotedJsonStrings(String json) {
    StrictJsonObject object = StrictJsonObject.parse(json, RESOURCE);

    assertThatThrownBy(() -> object.string("value"))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("double-quoted JSON string");
  }

  private static Stream<String> invalidStringValues() {
    return Stream.of("{\"value\":plain}", "{\"value\":1}", "{\"value\":'single'}");
  }

  @Test
  void rejectsInvalidAndOverflowingIntegers() {
    StrictJsonObject string = StrictJsonObject.parse("{\"value\":\"1\"}", RESOURCE);
    StrictJsonObject plain = StrictJsonObject.parse("{\"value\":plain}", RESOURCE);
    StrictJsonObject collection = StrictJsonObject.parse("{\"value\":[]}", RESOURCE);
    StrictJsonObject styledInteger =
        StrictJsonObject.parse("{\"value\":!!int \"1\"}", RESOURCE);
    StrictJsonObject overflow =
        StrictJsonObject.parse("{\"value\":999999999999999999999}", RESOURCE);

    assertThatThrownBy(() -> string.integer("value"))
        .hasMessageContaining("must be a JSON integer");
    assertThatThrownBy(() -> plain.integer("value"))
        .hasMessageContaining("must be a JSON integer");
    assertThatThrownBy(() -> collection.integer("value"))
        .hasMessageContaining("must be a JSON integer");
    assertThatThrownBy(() -> styledInteger.integer("value"))
        .hasMessageContaining("must be a JSON integer");
    assertThatThrownBy(() -> overflow.integer("value"))
        .hasMessageContaining("outside the supported integer range");
  }

  @Test
  void rejectsWrongObjectArrayAndElementShapes() {
    StrictJsonObject object =
        StrictJsonObject.parse(
            "{\"object\":[],\"array\":{},\"objects\":[\"not-object\"]}", RESOURCE);

    assertThatThrownBy(() -> object.object("object"))
        .hasMessageContaining("must be a JSON object");
    assertThatThrownBy(() -> object.strings("array"))
        .hasMessageContaining("must be a JSON array");
    assertThatThrownBy(() -> object.objects("objects"))
        .hasMessageContaining("[0] must be a JSON object");
    assertThatThrownBy(() -> object.string("missing"))
        .hasMessageContaining("is required");
    assertThatThrownBy(() -> object.string("object"))
        .hasMessageContaining("double-quoted JSON string");
  }

  @Test
  void rejectsYamlCollectionSyntaxAndAnchors() {
    assertThatThrownBy(() -> StrictJsonObject.parse("value: \"x\"", RESOURCE))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("JSON object syntax");
    assertThatThrownBy(() -> StrictJsonObject.parse("!!set {\"value\":null}", RESOURCE))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("JSON object syntax");
    assertThatThrownBy(
            () -> StrictJsonObject.parse("{\"value\":&legacy \"x\"}", RESOURCE))
        .isInstanceOf(InitializationFailureException.class)
        .hasMessageContaining("must not contain YAML anchors");
  }
}
