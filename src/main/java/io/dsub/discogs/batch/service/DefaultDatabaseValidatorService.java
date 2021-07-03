package io.dsub.discogs.batch.service;

import io.dsub.discogs.batch.argument.DBType;
import io.dsub.discogs.batch.argument.validator.DefaultValidationResult;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.exception.DriverLoadFailureException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Simple class to perform testings to ensure compatibility.
 */
@Slf4j
public class DefaultDatabaseValidatorService implements DatabaseValidatorService {

    private static final String URL = "url";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final Pattern JDBC_PATTERN = Pattern.compile("jdbc:(\\w*)://.*");

    private static record Credentials(String url, String username, String password) {
    }

    /**
     * validates database connectivity and compatibility from given url, username and password.
     *
     * @param url      a jdbc url
     * @param username a username
     * @param password a password
     * @return validation result.
     */
    @Override
    public ValidationResult validate(String url, String username, String password) {
        ValidationResult result = new DefaultValidationResult();
        username = username == null ? "" : encode(username);
        password = password == null ? "" : encode(password);
        if (url == null || url.isBlank()) {
            return result.withIssue("url cannot be null or blank");
        }
        Credentials credentials = new Credentials(url, username, password);
        return doValidate(credentials);
    }

    /**
     * validates database connectivity and compatibility from given {@link ApplicationArguments}.
     * validation will be depending on following option argument values:
     * 1. url
     * 2. username
     * 3. password
     * <p>
     * the argument names must match, and should be present in <i><b>optional</b></i> argument.
     * if multiple values are present, only the first value will be examined.
     *
     * @param args a {@link ApplicationArguments} that contains url, username, password as option argument.
     * @return validation result.
     */
    @Override
    public ValidationResult validate(ApplicationArguments args) {
        String urlValue = getOptionValue(args, URL);
        String usernameValue = getOptionValue(args, USERNAME);
        String passwordValue = getOptionValue(args, PASSWORD);
        return validate(urlValue, usernameValue, passwordValue);
    }

    private ValidationResult checkMetaData(DatabaseMetaData meta) {
        ValidationResult result = new DefaultValidationResult();
        try {
            String productName = meta.getDatabaseProductName().toLowerCase();
            int majorVersion = meta.getDatabaseMajorVersion();
            int minorVersion = meta.getDatabaseMinorVersion();

            DBType type = DBType.getTypeOf(productName);

            if (type == null) {
                return result.withIssue(getUnsupportedProductMessage(productName));
            }

            if (type.equals(DBType.PostgreSQL)) {
                if (majorVersion < 9 || (majorVersion == 9 && minorVersion < 5)) {
                    return result.withIssue(productName + " version below 9.5 is not supported.");
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            String msg = "failed to check database metadata by: " + e.getSQLState();
            msg += ". errorCode: " + e.getErrorCode() + ". message: " + e.getMessage();
            return result.withIssue(msg);
        }
        return result;
    }

    protected Connection getConnection(Credentials credentials) throws SQLException {
        return DriverManager.getConnection(credentials.url(), credentials.username(), credentials.password());
    }

    /**
     * actual validation from the credentials.
     *
     * <p>
     *     failure points can be one of the followings:
     *     <ul>
     *         <li>if database product is unknown</li>
     *         <li>if driver acquisition failed</li>
     *         <li>if connection acquisition failed</li>
     *         <li>if database metadata acquisition failed</li>
     *         <li>if the metadata shows incompatibility</li>
     *     </ul>
     * </p>
     *
     * @param credentials to be evaluated.
     * @return validation result
     */
    protected ValidationResult doValidate(Credentials credentials) {
        ValidationResult result = new DefaultValidationResult();

        int defaultTimeOut = DriverManager.getLoginTimeout(); // default

        try {
            tryLoadDriver(credentials);
        } catch (DriverLoadFailureException ex) {
            log.error(ex.getMessage());
            return result.withIssue("failed to allocate driver for url: " + credentials.url());
        }

        DatabaseMetaData metaData = null;

        try (Connection conn = getConnection(credentials)) {
            metaData = conn.getMetaData();
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message.contains("server time zone")) {
                return result.withIssue(
                        "database need to be configured with UTC server time zone. please set and try again.");
            }
            return result.withIssue("failed to test connection! " + e.getMessage().toLowerCase());
        }

        result.combine(checkMetaData(metaData));
        DriverManager.setLoginTimeout(defaultTimeOut); // restore

        return result;
    }

    private void tryLoadDriver(Credentials credentials) throws DriverLoadFailureException {
        String url = credentials.url();
        String driverClassName = getDriverClassName(url);
        try {
            if (driverClassName == null) {
                throw new DriverLoadFailureException("failed to recognize database product name from " + url);
            }
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new DriverLoadFailureException(driverClassName + " is not present on the classpath.");
        }
    }

    private String getProductName(String url) throws DriverLoadFailureException {
        if (url == null) {
            throw new DriverLoadFailureException("url cannot be null or blank url");
        }
        Matcher matcher = JDBC_PATTERN.matcher(url);
        String prodName = null;
        if (matcher.matches()) {
            prodName = matcher.group(1);
        }
        if (prodName == null || prodName.isBlank()) {
            throw new DriverLoadFailureException("failed to recognize database product name from " + url);
        }
        return prodName;
    }

    private String getDriverClassName(String url) throws DriverLoadFailureException {
        String prodName = getProductName(url);
        DBType type = DBType.getTypeOf(prodName);
        if (type == null) {
            throw new DriverLoadFailureException(getUnsupportedProductMessage(prodName));
        }
        return type.getDriverClassName();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String getOptionValue(ApplicationArguments args, String key) {
        if (args.containsOption(key)) {
            return args.getOptionValues(key).stream().findFirst().orElse(null);
        }
        return null;
    }

    private String getUnsupportedProductMessage(String productName) {
        String msg = productName + " is currently supported. currently supported: [";
        msg += Arrays.stream(DBType.values()).map(DBType::name).collect(Collectors.joining(" | "));
        return msg + "]";
    }
}
