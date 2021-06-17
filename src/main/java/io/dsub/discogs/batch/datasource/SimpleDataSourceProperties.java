package io.dsub.discogs.batch.datasource;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.exception.MissingRequiredArgumentException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.ApplicationArguments;

/** Class to share data source related properties. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
      throw new MissingRequiredArgumentException( // todo: track related issues
          "application argument must contain url in the non-option argument.");
    }
    connectionUrl = map.get(ArgType.URL.getGlobalName());
    dbType = DBType.getDBType(connectionUrl);
    username = map.get(ArgType.USERNAME.getGlobalName());
    password = map.get(ArgType.PASSWORD.getGlobalName());
  }
}
