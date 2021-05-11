package io.dsub.discogsdata.batch.argument;

import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Argument property to be mapped to each {@link ArgType} instance. */
@Getter
@AllArgsConstructor
public class ArgumentProperty {

  private final String globalName;
  private final List<String> synonyms;
  private final boolean required;
  private final int minValuesCount;
  private final int maxValuesCount;
  private final Class<?> supportedType;

  /*
   * Boiler plates for builder pattern.
   */
  public static ArgumentPropertyBuilder builder() {
    return new ArgumentPropertyBuilder();
  }

  public boolean contains(String argName) {
    String target = argName.replaceAll("[_-]", "");
    return this.getSynonyms().stream()
        .map(String::toLowerCase)
        .anyMatch(s -> s.equalsIgnoreCase(target));
  }

  public static class ArgumentPropertyBuilder {
    private String globalName;
    private List<String> synonyms;
    private Boolean required;
    private Integer minValuesCount;
    private Integer maxValuesCount;
    private Class<?> supportedType;

    ArgumentPropertyBuilder() {}

    public ArgumentPropertyBuilder globalName(String globalName) {
      this.globalName = globalName;
      return this;
    }

    public ArgumentPropertyBuilder synonyms(List<String> synonyms) {
      if (synonyms == null || synonyms.isEmpty()) {
        return this;
      }
      if (this.synonyms == null) {
        this.synonyms = new ArrayList<>();
      }

      this.synonyms.addAll(synonyms.stream().filter(Objects::nonNull).collect(Collectors.toList()));
      return this;
    }

    public ArgumentPropertyBuilder synonyms(String... synonyms) {
      if (synonyms == null || synonyms.length == 0) {
        return this;
      }

      List<String> values =
          Arrays.stream(synonyms).filter(Objects::nonNull).collect(Collectors.toList());
      if (this.synonyms == null) {
        this.synonyms = new ArrayList<>();
      }
      this.synonyms.addAll(values);
      return this;
    }

    public ArgumentPropertyBuilder required(boolean required) {
      this.required = required;
      return this;
    }

    public ArgumentPropertyBuilder minValuesCount(int minValuesCount) {
      this.minValuesCount = minValuesCount;
      return this;
    }

    public ArgumentPropertyBuilder maxValuesCount(int maxValuesCount) {
      this.maxValuesCount = maxValuesCount;
      return this;
    }

    public ArgumentPropertyBuilder supportedType(Class<?> supportedType) {
      this.supportedType = supportedType;
      return this;
    }

    public ArgumentProperty build() {
      boolean required = this.required != null && this.required;
      int minValuesCount = this.minValuesCount == null ? 1 : this.minValuesCount;
      int maxValuesCount = this.maxValuesCount == null ? 1 : this.maxValuesCount;
      Class<?> supportedType = this.supportedType == null ? String.class : this.supportedType;

      if (globalName == null) {
        throw new InvalidArgumentException("global name should not be null");
      }
      if (minValuesCount > maxValuesCount) {
        throw new InvalidArgumentException("minValues cannot be greater than maxValues");
      }
      if (minValuesCount < 0) {
        throw new InvalidArgumentException("minValues cannot be lower than 0");
      }
      List<String> synonyms = new ArrayList<>();
      if (this.synonyms != null && !this.synonyms.isEmpty()) {
        synonyms.addAll(this.synonyms);
      }
      synonyms.add(globalName);
      return new ArgumentProperty(
          globalName, synonyms, required, minValuesCount, maxValuesCount, supportedType);
    }
  }
}
