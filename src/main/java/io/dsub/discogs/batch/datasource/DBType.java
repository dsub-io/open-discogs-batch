package io.dsub.discogs.batch.datasource;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum DBType {
  MYSQL("com.mysql.cj.jdbc.Driver"),
  POSTGRESQL("org.postgresql.Driver");

  private final String driverClassName;

  DBType(String driverClassName) {
    this.driverClassName = driverClassName;
  }

  public static String getDriverClassName(String connectionUrl) {
    return getDBType(connectionUrl).getDriverClassName();
  }

  public static DBType getDBType(String connectionUrl) {
    return Arrays.stream(values())
        .filter(type -> connectionUrl.matches("^jdbc:" + type.name().toLowerCase() + ".*"))
        .findFirst()
        .orElseThrow(
            () ->
                new InvalidArgumentException("failed to recognize DB type from " + connectionUrl));
  }

  public static List<String> getNames() {
    return Arrays.stream(DBType.values())
        .map(db -> db.name().toLowerCase())
        .collect(Collectors.toList());
  }

  public String getDriverClassName() {
    return driverClassName;
  }
}
