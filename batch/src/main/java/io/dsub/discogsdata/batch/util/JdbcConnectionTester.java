package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.argument.ArgType;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
  private static final String DEFAULT_JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

  /**
   * @param args arguments to be parsed (to obtain url, username and password)
   * @throws InvalidArgumentException will be thrown under following conditions:
   *     <p>1. {@link DriverManager#getConnection(String)} fails.
   *     <p>2. Any required parameters are missing.
   *     <p>3. Failure to load jdbc driver.
   */
  public void testConnection(String[] args) {
    testConnection(args, DEFAULT_JDBC_DRIVER);
  }

  /**
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
    String username = props.get(USERNAME);
    String password = props.get(PASSWORD);

    assert (url != null && username != null && password != null);

    Connection conn = null;

    try {
      Class.forName(jdbcDriverName);
      conn = DriverManager.getConnection(url, username, password);
    } catch (ClassNotFoundException e) {
      throw new InvalidArgumentException(
          "failed to load jdbc driver. check driver name again! "
              + "(example: com.mysql.cj.jdbc.Driver)");
    } catch (SQLException e) {
      throw new InvalidArgumentException(
          "failed to retrieve connection! check your url, username, password again");
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
}
