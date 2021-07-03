package io.dsub.discogs.batch.argument.validator;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.argument.DBType;
import org.springframework.boot.ApplicationArguments;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A validator to check necessary information to perform initialization of {@link
 * javax.sql.DataSource}. The validator will basically require three arguments: user, pass and url.
 * First it will check the existence and duplication of each arguments. Next it will decide if
 * additional validation can be performed. Finally, it will resolve the url format by pattern to
 * check if something is wrong.
 *
 * <p>Results from the first step will be accumulated then returned if any issue exists. Final
 * validation for url will simply validate the format.
 */
public class DataSourceArgumentValidator implements ArgumentValidator {

    public static final List<ArgType> REQUIRED_TYPES = List.of(ArgType.URL, ArgType.USERNAME, ArgType.PASSWORD);
    public static final Pattern URL_PATTERN = Pattern.compile(
            "(?<jdbcGrp>(?<jdbcHead>jdbc)(?<jdbcTail>:))" +
                    "(?<typeGrp>(?<type>\\w+)(?<typeTail>://))" +
                    "(?<addr>[\\w.]+)(?<addrTail>:)" +
                    "(?<port>[1-9]\\d{0,4})" +
                    "(?<schemaGrp>(?<schemaHead>/)(?<schema>\\w+))([?&]\\w+=\\w+)*");

    /**
     * Validation for {@link ApplicationArguments} if necessary arguments do exist.
     *
     * @param args to be validated.
     * @return result of the validation.
     */
    @Override
    public ValidationResult validate(ApplicationArguments args) {
        ValidationResult result = new DefaultValidationResult();

        result = checkIfExists(args, result);
        if (!result.isValid()) { // some db connection argument is missing.
            return result; // return as no more verification is meaningful.
        }

        result = checkDuplicates(args, result);
        if (!result.isValid()) { // duplications found on specific required args.
            return result; // return as no more verification is meaningful.
        }

        return checkUrl(args, result);
    }

    /**
     * Validation of datasource url by pattern.
     *
     * @param args             expected to hold url entry.
     * @param validationResult accumulated result of the validation.
     * @return final accumulation of validation result.
     */
    private ValidationResult checkUrl(ApplicationArguments args, ValidationResult validationResult) {
        String jdbcConnectionString =
                args.getNonOptionArgs().stream()
                        .filter(s -> s.startsWith(ArgType.URL.getGlobalName()))
                        .map(s -> s.substring(s.indexOf('=') + 1))
                        .findFirst()
                        .orElse(null);
        if (jdbcConnectionString == null) {
            return validationResult.withIssues("argument %s is missing", ArgType.URL.getGlobalName());
        }
        // if url doesn't match to required format
        if (!isValidConnURL(jdbcConnectionString)) {
            return validationResult.withIssues(getMalformedURLIssue());
        }
        return validationResult;
    }

    private boolean isValidConnURL(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        Matcher matcher = URL_PATTERN.matcher(url);
        return matcher.matches();
    }

    private boolean contains(Matcher matcher, String grpName) {
        try {
            return !matcher.group(grpName).isBlank();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private String getMalformedURLIssue() {
        return "invalid url format. expected: jdbc:"
                + DBType.getNames()
                + "://{address}:{port}/schema_name{?option=value}";
    }

    /**
     * Validation of the existence of the required entries.
     *
     * @param args   to be validated.
     * @param result {@link ValidationResult} to be accumulated.
     * @return accumulated issues during the validation.
     */
    private ValidationResult checkIfExists(ApplicationArguments args, ValidationResult result) {

        // get types that are present in args.
        List<ArgType> foundTypes =
                args.getNonOptionArgs().stream()
                        .map(arg -> arg.split("=")[0])
                        .map(ArgType::getTypeOf)
                        .filter(Objects::nonNull)
                        .filter(REQUIRED_TYPES::contains)
                        .collect(Collectors.toList());

        // find missing arguments by comparison.
        List<String> missingArgumentIssues =
                REQUIRED_TYPES.stream()
                        .filter(requiredType -> !foundTypes.contains(requiredType))
                        .map(missingType -> missingType.getGlobalName() + " argument is missing")
                        .collect(Collectors.toList());

        // returns the result
        return result.withIssues(missingArgumentIssues);
    }

    /**
     * Validates duplication of required argument types.
     *
     * @param args   to be validated.
     * @param result {@link ValidationResult} to be accumulated.
     * @return accumulated issues during the validation.
     */
    private ValidationResult checkDuplicates(ApplicationArguments args, ValidationResult result) {

        // filtered arguments of required type only.
        Map<ArgType, List<String>> targetArguments =
                args.getNonOptionArgs().stream()
                        .filter(
                                argString ->
                                        REQUIRED_TYPES.stream()
                                                .anyMatch(
                                                        argType -> argType.getSynonyms().contains(argString.split("=")[0])))
                        .collect(
                                Collectors.groupingBy(argString -> ArgType.getTypeOf(argString.split("=")[0])));

        List<String> issuesList =
                targetArguments.entrySet().stream()
                        .filter(argTypeListEntry -> argTypeListEntry.getValue().size() > 1)
                        .map(entry -> entry.getKey().getGlobalName() + " argument has duplicated entries")
                        .collect(Collectors.toList());

        return result.withIssues(issuesList);
    }
}
