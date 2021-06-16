package io.dsub.discogs.batch.datasource;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.exception.MissingRequiredArgumentException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.boot.ApplicationArguments;

/** Class to share data source related properties. */
@Data
public class SimpleDataSourceProperties implements DataSourceProperties {

  private String username;
  private String password;
  private String connectionUrl;
  private DBType dbType;

  public SimpleDataSourceProperties(ApplicationArguments args)
      throws MissingRequiredArgumentException {
    Map<String, String> map =
        args.getNonOptionArgs().stream()
            .map(s -> s.split("="))
            .collect(
                Collectors.toMap(
                    parts -> parts[0],
                    parts -> Arrays.stream(parts).skip(1).collect(Collectors.joining())));
    if (!map.containsKey(ArgType.URL.getGlobalName())) {
      throw new MissingRequiredArgumentException(
          "application argument must contain url in the non-option argument.");
    }
    connectionUrl = map.get(ArgType.URL.getGlobalName());
    dbType = DBType.getDBType(connectionUrl);
    username = map.get(ArgType.USERNAME.getGlobalName());
    password = map.get(ArgType.PASSWORD.getGlobalName());
  }

  SimpleDataSourceProperties(
      String username, String password, String connectionUrl, DBType dbType) {
    this.username = username;
    this.password = password;
    this.connectionUrl = connectionUrl;
    this.dbType = dbType;
  }

  public static SimpleDataSourcePropertiesBuilder builder() {
    return new SimpleDataSourcePropertiesBuilder();
  }

  public static class SimpleDataSourcePropertiesBuilder {

    private String username;
    private String password;
    private String connectionUrl;
    private DBType dbType;

    SimpleDataSourcePropertiesBuilder() {}

    public SimpleDataSourcePropertiesBuilder username(String username) {
      this.username = username;
      return this;
    }

    public SimpleDataSourcePropertiesBuilder password(String password) {
      this.password = password;
      return this;
    }

    public SimpleDataSourcePropertiesBuilder connectionUrl(String connectionUrl) {
      this.connectionUrl = connectionUrl;
      return this;
    }

    public SimpleDataSourcePropertiesBuilder dbType(DBType dbType) {
      this.dbType = dbType;
      return this;
    }

    public SimpleDataSourceProperties build() {
      return new SimpleDataSourceProperties(username, password, connectionUrl, dbType);
    }

    public String toString() {
      return "SimpleDataSourceProperties.SimpleDataSourcePropertiesBuilder(username="
          + this.username
          + ", password="
          + this.password
          + ", connectionUrl="
          + this.connectionUrl
          + ", dbType="
          + this.dbType
          + ")";
    }
  }
}
