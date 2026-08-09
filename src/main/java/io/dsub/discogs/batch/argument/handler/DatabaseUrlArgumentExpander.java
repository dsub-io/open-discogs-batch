package io.dsub.discogs.batch.argument.handler;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DatabaseUrlArgumentExpander {

  String[] expand(String[] sourceArgs) {
    List<String> result = new ArrayList<>();
    for (String argument : sourceArgs) {
      String[] parts = argument.split("=", 2);
      String name = parts[0].replaceFirst("^-+", "").replaceAll("[-_]", "");
      if (!name.equalsIgnoreCase("databaseurl")) {
        result.add(argument);
        continue;
      }
      if (parts.length != 2 || parts[1].isBlank()) {
        throw new InvalidArgumentException("database-url requires a value");
      }
      DatabaseConnection connection = parse(parts[1]);
      result.add("--url=" + connection.jdbcUrl());
      result.add("--username=" + connection.username());
      result.add("--password=" + connection.password());
    }
    return result.toArray(String[]::new);
  }

  private DatabaseConnection parse(String value) {
    try {
      URI uri = new URI(value);
      String scheme = uri.getScheme();
      if (scheme == null
          || !(scheme.equalsIgnoreCase("postgres") || scheme.equalsIgnoreCase("postgresql"))) {
        throw invalid();
      }
      if (uri.getHost() == null || uri.getHost().isBlank()) {
        throw invalid();
      }
      String userInfo = uri.getRawUserInfo();
      if (userInfo == null || !userInfo.contains(":")) {
        throw new InvalidArgumentException(
            "database-url must include percent-encoded username and password");
      }
      String[] credentials = userInfo.split(":", 2);
      String path = uri.getRawPath();
      if (path == null || path.length() <= 1) {
        throw new InvalidArgumentException("database-url must include a database name");
      }

      String host = uri.getHost().contains(":") ? "[" + uri.getHost() + "]" : uri.getHost();
      StringBuilder jdbcUrl =
          new StringBuilder("jdbc:postgresql://").append(host.toLowerCase(Locale.ROOT));
      if (uri.getPort() >= 0) {
        jdbcUrl.append(':').append(uri.getPort());
      }
      jdbcUrl.append(path);
      if (uri.getRawQuery() != null) {
        jdbcUrl.append('?').append(uri.getRawQuery());
      }
      return new DatabaseConnection(
          jdbcUrl.toString(), decode(credentials[0]), decode(credentials[1]));
    } catch (URISyntaxException exception) {
      throw new InvalidArgumentException("database-url is not a valid PostgreSQL URI");
    }
  }

  private String decode(String value) {
    return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
  }

  private InvalidArgumentException invalid() {
    return new InvalidArgumentException(
        "database-url must use postgresql://user:password@host:port/database format");
  }

  private record DatabaseConnection(String jdbcUrl, String username, String password) {}
}
