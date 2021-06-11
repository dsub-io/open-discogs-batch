package io.dsub.discogs.batch.util;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.common.exception.InvalidArgumentException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Simple class to perform tangible testing against retrieving connection to a database. The {@link
 * #testConnection(String[], String)} or {@link #testConnection(String[])} will be used to actual
 * evaluation of retrieving a connection string, which will either throw an exception or execute
 * silently.
 */
public class JdbcConnectionTester {

  private static final String URL = "url";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String MYSQL_CJ_JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
  private static final String POSTGRESQL_JDBC_DRIVER = "org.postgresql.Driver";

  /**
   * Auto resolves jdbc driver matching the url argument to test.
   *
   * @param args arguments to be parsed (to obtain url, username and password)
   * @throws InvalidArgumentException will be thrown under following conditions:
   *     <p>1. {@link DriverManager#getConnection(String)} fails.
   *     <p>2. Any required parameters are missing.
   *     <p>3. Failure to load jdbc driver.
   */
  public void testConnection(String[] args) {
    testConnection(args, autoResolveJdbcDriver(args));
  }

  /**
   * Test the connection from given arguments and jdbcDriverName.
   *
   * @param args arguments to be parsed (to obtain url, username and password)
   * @param jdbcDriverName driver class name. this will be default to com.mysql.cj.jdbc.Driver.
   * @throws InvalidArgumentException will be thrown under following conditions:
   *     <p>1. {@link DriverManager#getConnection(String)} fails.
   *     <p>2. Any required parameters are missing.
   *     <p>3. Failure to load jdbc driver.
   */
  public void testConnection(String[] args, String jdbcDriverName) throws InvalidArgumentException {

    // InvalidArgumentException will be thrown if any among url, username or password is missing.
    Map<String, String> props = parseJdbcArgs(new DefaultApplicationArguments(args));
    String url = props.get(URL);
    String username = encode(props.get(USERNAME));
    String password = encode(props.get(PASSWORD));

    assert (url != null && username != null && password != null);

    Connection conn = null;
    int defaultTimeOut = DriverManager.getLoginTimeout(); // default

    try {
      Class.forName(jdbcDriverName);
      DriverManager.setLoginTimeout(2); // set temporary login timeout
      conn =
          DriverManager.getConnection(url, username, password); // test if connection is available
      DriverManager.setLoginTimeout(defaultTimeOut); // restore
    } catch (ClassNotFoundException e) {
      throw new InvalidArgumentException(
          "failed to load jdbc driver. check driver name again! "
              + "(example: com.mysql.cj.jdbc.Driver)");
    } catch (SQLException e) {

      String message = e.getMessage();
      if (message.contains("server time zone")) {
        throw new InvalidArgumentException(
            "database need to be configured with UTC server time zone. please set and try again.");
      }
      throw new InvalidArgumentException(
          "failed to test connection! " + e.getMessage().toLowerCase());
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException ignored) {
        }
      }
    }
  }

  /**
   * Parse url, username and password from give {@link ApplicationArguments}. The naming convention
   * to be used during parse will depends on {@link ArgType}.
   *
   * <p>It is also important to note that the values to be extracted are supposed to be
   * nonOptionArg, which does not have any flags (normally identified as '-' or '--')
   *
   * @param args argument source.
   * @return result of parse.
   */
  public Map<String, String> parseJdbcArgs(ApplicationArguments args)
      throws InvalidArgumentException {
    Map<String, String> result = new HashMap<>();
    for (String nonOptionArg : args.getNonOptionArgs()) {
      int idx = nonOptionArg.indexOf('=');
      if (idx == -1) { // not found the '=' character
        idx = nonOptionArg.length();
      }
      String name = nonOptionArg.substring(0, idx);
      ArgType type = ArgType.getTypeOf(name);

      if (type == null) {
        continue;
      }

      boolean isOutOfBounds = idx >= nonOptionArg.length();

      String value = "";

      if (isOutOfBounds && !type.equals(ArgType.PASSWORD)) {
        throw new InvalidArgumentException("missing argument for " + type.getGlobalName());
      } else if (isOutOfBounds) {
        value = "";
      } else {
        value = nonOptionArg.substring(idx + 1);
      }

      String argName = type.getGlobalName();
      if (argName.equals(URL) || argName.equals(USERNAME) || argName.equals(PASSWORD)) {
        result.put(type.getGlobalName(), value);
      }
    }
    if (result.size() < 3) {
      String missingKeys =
          List.of(URL, PASSWORD, USERNAME).stream()
              .filter(key -> !result.containsKey(key))
              .collect(Collectors.joining(", "));
      throw new InvalidArgumentException("missing required jdbc values: " + missingKeys);
    }
    return result;
  }

  /**
   * auto resolves given string[] args to obtain driver class name.
   *
   * @param args to be extracted.
   * @return driver class name. default is com.mysql.cj.jdbc.Driver.
   */
  public String autoResolveJdbcDriver(String[] args) {
    Map<String, String> jdbcArgs = parseJdbcArgs(new DefaultApplicationArguments(args));
    String url = jdbcArgs.get(URL);
    if (url.matches(".*postgresql.*")) {
      return POSTGRESQL_JDBC_DRIVER;
    }
    return MYSQL_CJ_JDBC_DRIVER;
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
