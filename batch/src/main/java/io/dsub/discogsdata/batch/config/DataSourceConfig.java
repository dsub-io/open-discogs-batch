package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.common.exception.MissingRequiredArgumentException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

  private static final String PLAIN_JDBC_URL_PATTERN_STRING =
      "^jdbc:mysql://[\\w._-]+:[1-9][0-9]{0,4}/[\\w_-]+$";

  private static final String TIME_ZONE_UTC_OPT = "serverTimeZone=UTC";
  private static final String CACHE_PREP_STMT_OPT = "cachePrepStmts=true";
  private static final String USE_SERVER_PREP_STMTS_OPT = "useServerPrepStmts=true";
  private static final String REWRITE_BATCHED_STMTS_OPT = "rewriteBatchedStatements=true";

  private static final Pattern PLAIN_JDBC_URL_PATTERN =
      Pattern.compile(PLAIN_JDBC_URL_PATTERN_STRING, Pattern.CASE_INSENSITIVE);

  private static final Pattern CONTAINS_OPTION_QUESTION_MARK_PATTERN = Pattern.compile(".*?.*");

  private final ApplicationArguments applicationArguments;

  @Bean(name = "batchDataSource")
  public DataSource batchDataSource() {

    // map nonOptional args..
    Map<String, String> arguments =
        applicationArguments.getNonOptionArgs().stream()
            .map(s -> s.split("="))
            .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));

    if (!arguments.containsKey("url")) {
      throw new MissingRequiredArgumentException(
          "application argument must contain url in the non-option argument.");
    }

    String optAppliedUrlString = appendOptions(arguments.get("url"));

    return DataSourceBuilder.create()
        .url(optAppliedUrlString)
        .username(arguments.get("username"))
        .password(arguments.get("password"))
        .driverClassName("com.mysql.cj.jdbc.Driver")
        .build();
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
          .concat(
              String.join(
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
