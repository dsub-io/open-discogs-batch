package io.dsub.discogs.batch.argument.formatter;

import io.dsub.discogs.batch.datasource.DBType;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * A convenience class that performs jdbc url formatter. Will remain deprecated until need arises
 * again, or tbd to be removed.
 */
@Slf4j
public class JdbcUrlFormatter implements ArgumentFormatter {

  private static final Pattern JDBC_URL_PATTERN = Pattern.compile(
      "jdbc:(\\w+)://[.\\w]+:[\\d]+(/\\w+)?.*"
  );

  private static final Pattern JDBC_OPTION_PATTERN = Pattern.compile(".*(\\?).*");

  /* JDBC CONNECTION OPTIONS  */
  private static final String TIME_ZONE_UTC_OPT = "serverTimeZone=UTC";
  private static final String CACHE_PREP_STMT_OPT = "cachePrepStmts=true";
  private static final String USE_SERVER_PREP_STMTS_OPT = "useServerPrepStmts=true";
  private static final String REWRITE_BATCHED_STMTS_OPT = "rewriteBatchedStatements=true";
  private static final String NO_LEGACY_DATE_TIME_CODE_OPT = "useLegacyDatetimeCode=false";

  /* HEADER */
  private static final String URL_HEADER = "url=";

  @Override
  public String[] format(String[] args) {
    if (args == null || args.length == 0) {
      return args;
    }
    return Arrays.stream(args)
        .map(arg -> arg.startsWith(URL_HEADER) ? doFormat(arg) : arg)
        .toArray(String[]::new);
  }

  private String doFormat(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }

    url = url.replace(URL_HEADER, ""); // removes header

    Matcher m = JDBC_URL_PATTERN.matcher(url);

    boolean patternMatches = m.matches();

    if (patternMatches && (m.group(2) == null || m.group(2).isBlank())) {
      log.info(
          "default database or schema missing. appending default schema \"discogs\" to jdbc url");

      String[] parts = url.split("\\?");

      url = parts[0] + "/discogs";

      if (parts.length > 1) {
        url = url + "?" + parts[1];
      }
    }

    if (patternMatches) {
      String databaseProductName = m.group(1);
      DBType type = DBType.getTypeOf(databaseProductName);
      if (type == null) {
        return URL_HEADER + url;
      }
    }

    return URL_HEADER + appendOptions(url);
  }

  private boolean isOptionPresent(String url) {
    return JDBC_OPTION_PATTERN.matcher(url).matches();
  }

  private String appendOptions(String url) {
    String amp = "&";
    String q = "?";
    String header = isOptionPresent(url) ? amp : q;

    if (!url.matches(".*(?i)serverTimezone.*")) {
      url += header + TIME_ZONE_UTC_OPT;
      header = amp;
    }
    if (!url.matches(".*(?i)(cachePrepStmts).*")) {
      url += header + CACHE_PREP_STMT_OPT;
      header = amp;
    }
    if (!url.matches(".*(?i)rewriteBatchedStatements.*")) {
      url += header + REWRITE_BATCHED_STMTS_OPT;
      header = amp;
    }
    if (!url.matches(".*(?i)(useServerPrepStmts).*")) {
      url += header + USE_SERVER_PREP_STMTS_OPT;
      header = amp;
    }
    if (!url.matches(".*(?i)(useLegacyDatetimeCode).*")) {
      url += header + NO_LEGACY_DATE_TIME_CODE_OPT;
    }

    return url;
  }
}
