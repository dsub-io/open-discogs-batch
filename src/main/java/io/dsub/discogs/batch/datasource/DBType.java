package io.dsub.discogs.batch.datasource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@RequiredArgsConstructor
public enum DBType {
  POSTGRESQL("org.postgresql.Driver");

  @Getter
  private final String driverClassName;

  public static List<String> getNames() {
    return Arrays.stream(values())
        .map(DBType::name)
        .map(String::toLowerCase)
        .collect(Collectors.toList());
  }

  public static DBType getTypeOf(String from) {
    String target = from.toLowerCase();
    return Arrays.stream(values())
        .filter(type -> type.value().equals(target))
        .findFirst()
        .orElse(null);
  }

  public String value() {
    return this.name().toLowerCase();
  }
}
