package io.dsub.discogs.batch.argument.formatter;

import io.dsub.discogs.batch.argument.DBType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link ArgumentFormatter} that formats url entry into Jdbc connection string.
 */
public class JdbcUrlFormatter implements ArgumentFormatter {

    private static final String DB_NAMES = "(" + String.join("|", DBType.getNames()) + ")";

    /*
     * options for connection url
     */
    private static final String TIME_ZONE_UTC_OPT = "serverTimeZone=UTC";
    private static final String CACHE_PREP_STMT_OPT = "cachePrepStmts=true";
    private static final String USE_SERVER_PREP_STMTS_OPT = "useServerPrepStmts=true";
    private static final String REWRITE_BATCHED_STMTS_OPT = "rewriteBatchedStatements=true";
    private static final String NO_LEGACY_DATE_TIME_CODE = "useLegacyDatetimeCode=false";

    public static final Pattern URL_PATTERN = Pattern.compile(
            "(?<jdbcGrp>(?<jdbcHead>jdbc)?(?<jdbcTail>:)?)?" +
                    "(?<type>\\w+://)?" +
                    "(?<addr>\\w+)?" +
                    "(?<port>:[1-9]\\d{0,4})?" +
                    "(?<schemaGrp>(?<schemaHead>/)?(?<schema>\\w+)?)?" +
                    "(?<optGrp>(?<initOptHead>\\?)(?<initOpt>\\w+=\\w+)((?<optionHead>&)(?<option>\\w+=\\w+))*)?");

    public static final Pattern KNOWN_DB_PATTERN = Pattern.compile(".*" + DB_NAMES + ".*");

    public static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "^(localhost|\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|\\w+(\\.[^.]+)*)$", Pattern.CASE_INSENSITIVE);

    public static final Pattern EQUALS = Pattern.compile("^url=.*", Pattern.CASE_INSENSITIVE);

    /**
     * Formats argument to be proper jdbc connection string if marked as url entry.
     *
     * @param arg argument to be evaluated.
     * @return as a jdbc connection string if arg is marked as a url entry. If not, it will simply
     * return the input argument as-is.
     */
    @Override
    public String format(String arg) {

        if (!EQUALS.matcher(arg).matches()) {
            return arg;
        }

        int eqIdx = arg.indexOf('=');
        String urlValue = arg.substring(eqIdx + 1);
        int bIdx = urlValue.indexOf(' ');
        urlValue = urlValue.substring(0, bIdx < 0 ? urlValue.length() : bIdx);

        Matcher matcher = URL_PATTERN.matcher(urlValue);
        if (!matcher.matches()) {
            return arg;
        }

        String typeInput = getType(matcher);
        String url = "jdbc:";
        String type = isKnownType(typeInput) ? typeInput : "mysql";
        String address = getAddress(matcher.group("addr"));
        String port = getPort(matcher.group("port"), type);
        String schema = getSchema(matcher.group("schema"));
        String compiled = url + type + "://" + address + ":" + port + "/" + schema;

        return "url=" + appendOptionsTo(compiled, matcher);
    }

    private String getType(Matcher matcher) {
        String type = matcher.group("type");
        if (type == null) {
            return null;
        }
        return type.replaceAll("://", "");
    }

    private String getAddress(String address) {
        if (address != null && ADDRESS_PATTERN.matcher(address).matches()) {
            return address;
        }
        return "localhost";
    }

    private String getPort(String port, String type) {
        if (port != null && !port.isBlank()) {
            return port.replaceAll(":", "");
        }
        return switch (type) {
            case "mysql", "mariadb" -> "3306";
            case "postgresql" -> "5432";
            default -> "8080";
        };
    }

    private String getSchema(String schema) {
        if (schema != null && !schema.isBlank()) {
            return schema;
        }
        return "discogs_data";
    }

    private boolean isKnownType(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        return KNOWN_DB_PATTERN.matcher(type).matches();
    }

    /**
     * Appends required arguments for batch processing.
     *
     * <p>The options include timezone, cache statements, server statements and rewrite batch
     * statements.
     *
     * @param originalUrl given url that may or may not contain any of those options.
     * @return url that has all required options.
     */
    protected String appendOptionsTo(String originalUrl, Matcher matcher) {

        String optHead = matcher.group("initOptHead");
        String optGrp = matcher.group("optGrp");
        // check if options are missing
        if ((optGrp == null || optGrp.isBlank()) && optHead == null || optHead.isBlank()) {
            // append entire options
            return originalUrl
                    .concat("?")
                    .concat(String.join(
                            "&",
                            TIME_ZONE_UTC_OPT,
                            CACHE_PREP_STMT_OPT,
                            USE_SERVER_PREP_STMTS_OPT,
                            REWRITE_BATCHED_STMTS_OPT,
                            NO_LEGACY_DATE_TIME_CODE));
        }

        // option already exists. additional options accordingly.
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
        if (!originalUrl.contains("useLegacyDatetimeCode")) {
            originalUrl += "&" + NO_LEGACY_DATE_TIME_CODE;
        }
        return originalUrl;
    }
}
