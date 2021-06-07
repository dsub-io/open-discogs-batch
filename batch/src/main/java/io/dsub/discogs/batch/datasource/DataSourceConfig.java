package io.dsub.discogs.batch.datasource;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.common.exception.InitializationFailureException;
import io.dsub.discogs.common.exception.UnsupportedOperationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Getter
@Order(0)
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

  private static final String DB_VENDOR = "(" + String.join("|", DBType.getNames()) + ")";
  private static final String PLAIN_JDBC_URL_PATTERN_STRING =
      "^jdbc:" + DB_VENDOR + "://[\\w._-]+:[1-9][0-9]{0,4}/[\\w_-]+$";

  private static final String TIME_ZONE_UTC_OPT = "serverTimeZone=UTC";
  private static final String CACHE_PREP_STMT_OPT = "cachePrepStmts=true";
  private static final String USE_SERVER_PREP_STMTS_OPT = "useServerPrepStmts=true";
  private static final String REWRITE_BATCHED_STMTS_OPT = "rewriteBatchedStatements=true";

  private static final Pattern PLAIN_JDBC_URL_PATTERN =
      Pattern.compile(PLAIN_JDBC_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);

  private static final Pattern CONTAINS_OPTION_QUESTION_MARK_PATTERN = Pattern.compile(".*?.*");

  private final ApplicationArguments args;

  @Value("classpath:schema/mysql-schema.sql")
  private Resource mysqlSchema;
  @Value("classpath:schema/postgresql-schema.sql")
  private Resource postgresSchema;

  @Bean(name = "batchDataSource")
  public HikariDataSource batchDataSource() {
    DataSourceProperties dbProps = dataSourceProperties();
    Properties properties = new Properties();
    properties.setProperty("rewriteBatchedStatements", "true");
    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setJdbcUrl(dbProps.getConnectionUrl());
    hikariDataSource.setUsername(dbProps.getUsername());
    hikariDataSource.setPassword(dbProps.getPassword());
    hikariDataSource.setDataSourceProperties(properties);
    hikariDataSource.setDriverClassName(dbProps.getDbType().getDriverClassName());
    initializeSchema(hikariDataSource);
    return hikariDataSource;
  }

  @Bean
  public DataSourceProperties dataSourceProperties() {
    SimpleDataSourceProperties properties = new SimpleDataSourceProperties(args);
    String url = appendOptions(properties.getConnectionUrl());
    properties.setConnectionUrl(url);
    return properties;
  }

  /**
   * Initializes given {@link DataSource} on conditionally. If currently set DB_TYPE is either null
   * or unsupported, it will throw {@link UnsupportedOperationException}.
   *
   * @param dataSource to be initialized.
   */
  protected void initializeSchema(DataSource dataSource) {

    JdbcTemplate jdbcTemplate = getJdbcTemplate(dataSource);
    Resource schema = getSchemaResource();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(schema.getInputStream()))) {

      String sql = reader.lines().collect(Collectors.joining("\n"));
      List<String> queries = Arrays.stream(sql.split(";"))
          .map(String::trim)
          .collect(Collectors.toList());

      for (String query : queries) {
        jdbcTemplate.execute(query);
      }

    } catch (IOException e) {
      log.error("failed to initialize schema: " + e.getMessage());
      throw new InitializationFailureException(e.getMessage());
    }
  }

  protected JdbcTemplate getJdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  protected Resource getSchemaResource() {
    DBType type = dataSourceProperties().getDbType();
    Resource schema;
    if (type.equals(DBType.POSTGRESQL)) {
      schema = postgresSchema;
    } else {
      schema = mysqlSchema;
    }
    return schema;
  }

  /**
   * Appends required arguments for batch processing.
   * <p>
   * The options include timezone, cache statements, server statements and rewrite batch
   * statements.
   *
   * @param originalUrl given url that may or may not contain any of those options.
   * @return url that has all required options.
   */
  protected String appendOptions(String originalUrl) {
    // check if optional params applied
    if (PLAIN_JDBC_URL_PATTERN.matcher(originalUrl).matches()) {
      // append entire options
      return originalUrl
          .concat("?")
          .concat(String.join(
              "&",
              TIME_ZONE_UTC_OPT,
              CACHE_PREP_STMT_OPT,
              USE_SERVER_PREP_STMTS_OPT,
              REWRITE_BATCHED_STMTS_OPT));
    }

    if (!originalUrl.contains("serverTimezone=")) {
      originalUrl += "&" + TIME_ZONE_UTC_OPT;
    }
    if (!originalUrl.contains("cachePrepStmts")) {
      originalUrl += "&" + CACHE_PREP_STMT_OPT;
    }
    if (!originalUrl.contains("rewriteBatchedStatements")) {
      originalUrl += "&" + REWRITE_BATCHED_STMTS_OPT;
    }
    if (!originalUrl.contains("useServerPrepStmts")) {
      originalUrl += "&" + USE_SERVER_PREP_STMTS_OPT;
    }
    return originalUrl;
  }
}
