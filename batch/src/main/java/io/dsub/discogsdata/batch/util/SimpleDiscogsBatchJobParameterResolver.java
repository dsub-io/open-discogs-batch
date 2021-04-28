package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.config.AppConfig;
import io.dsub.discogsdata.batch.dump.DumpDependencyResolver;
import io.dsub.discogsdata.batch.dump.DumpItem;
import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.common.exception.DuplicatedJobParameterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor

// TODO: IMPLEMENT FLAG TO SKIP SOURCE PRUNE STEPS
public class SimpleDiscogsBatchJobParameterResolver implements DiscogsBatchJobParameterResolver {
    private static final String CHUNK_SIZE_KEY = "chunkSize";
    private static final String THROTTLE_LIMIT_KEY = "throttleLimit";
    private static final String YEAR_MONTH_KEY = "yearMonth";
    private static final String TYPE_KEY = "type";
    private static final String ETAG_KEY = "etag";
    private final DumpService dumpService;
    private final DumpDependencyResolver dumpDependencyResolver;

    @Override
    public JobParameters resolve(JobParameters jobParameters) {

        jobParameters = resolveDependencies(jobParameters);

        return new JobParametersBuilder(jobParameters)
                .addJobParameters(new JobParametersBuilder()
                        .addDate("UTC", Date.from(Instant.now()))
                        .toJobParameters())
                .toJobParameters();
    }

    public JobParameters resolveDependencies(JobParameters parameters) {

        JobParametersBuilder builder = new JobParametersBuilder();

        Map<String, JobParameter> jobParameterMap =
                getNormalizedJobParameterMap(parameters);

        resolveChunkSizeAndThrottleLimit(jobParameterMap);

        if (isValidEntryPresent(jobParameterMap, ETAG_KEY)) {
            log.info("found etag entry. overriding other parameters...");
            builder.addJobParameters(extractEtagList(jobParameterMap));
            return builder.toJobParameters();
        }

        boolean hasYearMonth = isValidYearMonthPresent(jobParameterMap);
        boolean hasType = isValidTypePresent(jobParameterMap);

        if (hasYearMonth && hasType) {
            return resolveByTypeAndYearMonth(builder, jobParameterMap);
        }

        if (hasType) {
            return resolveByType(builder, jobParameterMap);
        }

        if (hasYearMonth) {
            return resolveByYearMonth(builder, jobParameterMap);
        }

        dumpService.getLatestCompletedDumpSet().forEach(
                dump -> builder.addString(dump.getRootElementName(), dump.getETag()));

        return builder.toJobParameters();
    }

    private JobParameters resolveByYearMonth(JobParametersBuilder builder, Map<String, JobParameter> jobParameterMap) {
        dumpDependencyResolver.resolveByYearMonth((String) jobParameterMap.get(YEAR_MONTH_KEY).getValue())
                .forEach(dump -> builder.addString(dump.getRootElementName(), dump.getETag()));
        return builder.toJobParameters();
    }

    private JobParameters resolveByType(JobParametersBuilder builder, Map<String, JobParameter> jobParameterMap) {
        Collection<String> types = extractStrings(jobParameterMap, TYPE_KEY);
        dumpDependencyResolver.resolveByTypes(types).forEach(dump ->
                builder.addString(dump.getRootElementName(), dump.getETag()));
        return builder.toJobParameters();
    }

    private JobParameters resolveByTypeAndYearMonth(JobParametersBuilder builder, Map<String, JobParameter> jobParameterMap) {
        Collection<String> typeList = extractStrings(jobParameterMap, TYPE_KEY);
        List<DumpItem> resolvedList = dumpDependencyResolver.resolveByTypesAndYearMonth(
                typeList, String.valueOf(jobParameterMap.get(YEAR_MONTH_KEY)));
        for (DumpItem dump : resolvedList) {
            builder.addString(dump.getRootElementName(), dump.getETag());
        }
        return builder.toJobParameters();
    }

    private Collection<String> extractStrings(Map<String, JobParameter> jobParameterMap, String key) {
        String fullStr = (String) jobParameterMap.get(key).getValue();
        if (fullStr.isBlank()) {
            return new ArrayList<>();
        }
        return List.of(fullStr.split(","));
    }

    private boolean isValidTypePresent(Map<String, JobParameter> jobParameterMap) {
        return isValidEntryPresent(jobParameterMap, TYPE_KEY);
    }

    private boolean isValidYearMonthPresent(Map<String, JobParameter> jobParameterMap) {
        return isValidEntryPresent(jobParameterMap, YEAR_MONTH_KEY);
    }

    private Map<String, JobParameter> getNormalizedJobParameterMap(JobParameters parameters) {
        Map<String, JobParameter> jobParameterMap = new HashMap<>();
        parameters.getParameters().keySet()
                .forEach(key -> {
                    String normalizedKey = key
                            .trim()
                            .replaceAll("[_-]", "")
                            .replaceAll("s$", "")
                            .toLowerCase(Locale.ROOT);
                    if (jobParameterMap.containsKey(key)) {
                        throw new DuplicatedJobParameterException(
                                normalizedKey + " has duplicated entry of " + key);
                    }
                    jobParameterMap.put(key, parameters.getParameters().get(key));
                });
        return jobParameterMap;
    }

    private void resolveChunkSizeAndThrottleLimit(Map<String, JobParameter> map) {
        resolveIntegerValueEntry(map, CHUNK_SIZE_KEY, AppConfig.CHUNK_SIZE);
        resolveIntegerValueEntry(map, THROTTLE_LIMIT_KEY, AppConfig.THROTTLE_LIMIT);
    }

    private void resolveIntegerValueEntry(Map<String, JobParameter> map, String key, int target) {
        String foundKey = getMatchingKey(map, key);
        String name = toCamelCase(key);
        if (foundKey == null) {
            log.info("missing {} key. leaving to default: {}", name, target);
        } else {
            Object o = getMatchingValue(map, key, JobParameter.ParameterType.LONG);
            log.info("found {} entry.", name);
            Long parsed = parseLongOrReturnNull(o);
            if (parsed != null) {
                log.info("setting {} to {}", name, parsed);
                if (target == AppConfig.THROTTLE_LIMIT) {
                    AppConfig.THROTTLE_LIMIT = parsed.intValue();
                } else {
                    AppConfig.CHUNK_SIZE = parsed.intValue();
                }
            } else {
                log.info("failed to parse {} value. leaving to default: {}", name, target);
            }
        }
    }

    private String toCamelCase(String in) {
        if (in == null || in.isBlank()) {
            return in;
        }
        String[] spilt = in.split("_");

        spilt[0] = spilt[0].toLowerCase();

        return spilt[0] + Arrays.stream(spilt)
                .skip(1)
                .map(item -> String.valueOf(item.charAt(0)).toUpperCase() + item.substring(1).toLowerCase())
                .collect(Collectors.joining());
    }

    private Long parseLongOrReturnNull(Object o) {
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            log.error("failed to parse {} to long value", o);
            return null;
        }
    }


    private Object getMatchingValue(Map<String, JobParameter> map, String key, JobParameter.ParameterType type) {
        String foundKey = getMatchingKey(map, key);
        if (foundKey != null) {
            return map.get(foundKey);
        }
        return null;
    }

    private String getMatchingKey(Map<String, ?> target, String key) {
        return target.keySet()
                .parallelStream()
                .filter(item -> trimThenLowerCase(item).matches(key.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    private String trimThenLowerCase(String item) {
        return item.replaceAll("[_-]", "")
                .toLowerCase();
    }

    private boolean isValidEntryPresent(Map<String, JobParameter> jobParameterMap, String key) {
        String foundKey = getMatchingKey(jobParameterMap, key);
        if (foundKey == null) {
            log.info("{} entry not found.", key);
            return false;
        }
        String value = (String) jobParameterMap.get(foundKey).getValue();
        if (value.isBlank()) {
            log.info("found {} entry with empty value.", key);
            return false;
        }
        return true;
    }

    private JobParameters extractEtagList(Map<String, JobParameter> jobParameterMap) {
        String key = getMatchingKey(jobParameterMap, ETAG_KEY);
        String fullStr = (String) jobParameterMap.get(key).getValue();

        List<DumpItem> resolvedDumps = dumpDependencyResolver
                .resolveByETags(Arrays.asList(fullStr.split(",")));

        JobParametersBuilder builder = new JobParametersBuilder();

        resolvedDumps.forEach(
                dump -> builder.addString(dump.getRootElementName(), dump.getETag()));
        return builder.toJobParameters();
    }
}
