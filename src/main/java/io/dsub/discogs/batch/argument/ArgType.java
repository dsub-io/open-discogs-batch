package io.dsub.discogs.batch.argument;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Enum to represent current supported argument types.
 */
@RequiredArgsConstructor
public enum ArgType {
  ALLOW_DOWNGRADE(
      ArgumentProperty.builder()
          .globalName("allowDowngrade")
          .synonyms("allow-downgrade")
          .required(false)
          .maxValuesCount(0)
          .minValuesCount(0)
          .build()),
  CHUNK_SIZE(
      ArgumentProperty.builder()
          .globalName("chunkSize")
          .supportedType(Long.class)
          .synonyms("chunk-size", "b")
          .build()),
  CLEANUP(
      ArgumentProperty.builder()
          .globalName("cleanup")
          .synonyms("c")
          .required(false)
          .maxValuesCount(0)
          .minValuesCount(0)
          .build()),
  DATA_DIR(
      ArgumentProperty.builder()
          .globalName("dataDir")
          .synonyms("data-dir")
          .build()),
  DUMP_MONTH(
      ArgumentProperty.builder()
          .globalName("dumpMonth")
          .synonyms("dump-month", "m")
          .build()),
  FORCE(
      ArgumentProperty.builder()
          .globalName("force")
          .synonyms("f")
          .required(false)
          .maxValuesCount(0)
          .minValuesCount(0)
          .build()),
  PASSWORD(
      ArgumentProperty.builder()
          .globalName("password")
          .synonyms("password", "pass", "p")
          .required(true)
          .build()),
  TYPE(
      ArgumentProperty.builder()
          .globalName("entities")
          .synonyms("entity", "e")
          .maxValuesCount(4)
          .build()),
  URL(
      ArgumentProperty.builder()
          .globalName("url")
          .synonyms("database-url")
          .required(true)
          .build()),
  USERNAME(
      ArgumentProperty.builder()
          .globalName("username")
          .synonyms("username", "user", "u")
          .required(true)
          .build());

  // properties mapped to each enum instance.
  private final ArgumentProperty props;

  public static ArgType getTypeOf(String key) {
    if (key == null || key.isBlank()) {
      return null;
    }
    String target = key.toLowerCase();
    for (ArgType argType : ArgType.values()) {
      if (argType.props.contains(target)) {
        return argType;
      }
    }
    return null;
  }

  public static boolean contains(String key) {
    for (ArgType t : ArgType.values()) {
      if (t.props.contains(key)) {
        return true;
      }
    }
    return false;
  }

  public List<String> getSynonyms() {
    return List.copyOf(this.props.getSynonyms());
  }

  public boolean isValueRequired() {
    return this.props.getMinValuesCount() > 0;
  }

  public int getMinValuesCount() {
    return this.props.getMinValuesCount();
  }

  public int getMaxValuesCount() {
    return this.props.getMaxValuesCount();
  }

  public String getGlobalName() {
    return this.props.getGlobalName();
  }

  public boolean isRequired() {
    return this.props.isRequired();
  }

  public Class<?> getSupportedType() {
    return this.props.getSupportedType();
  }
}
