package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.dump.DumpItem;
import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.dump.DumpType;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DiscogsJobParametersValidator implements JobParametersValidator {

    private final DumpService dumpService;

    /**
     * Check the parameters meet whatever requirements are appropriate, and
     * throw an exception if not.
     *
     * @param parameters some {@link JobParameters} (can be {@code null})
     * @throws JobParametersInvalidException if the parameters are invalid
     */
    @Override
    public void validate(JobParameters parameters) throws JobParametersInvalidException {
        // launch by default (most recent, all four types of dump)
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        Long chunkSize = getLong(parameters, "chunkSize");
        String etag = getString(parameters, "etag");
        String type = getString(parameters, "type");
        String date = getString(parameters, "date");

        if (chunkSize != null && chunkSize <= 0) {
            throw new JobParametersInvalidException("chunkSize cannot be lower than or equal to 0");
        }

        // check etag is presented without other parameters
        if (etag != null && (date != null || type != null)) {
            throw new JobParametersInvalidException("etag cannot set with other parameters.");
        }

        if (etag != null && etag.replaceAll(",", "").trim().isBlank()) {
            throw new JobParametersInvalidException("etag cannot be blank");
        }

        if (etag != null) {
            String[] etagList = etag.split(",");
            for (String etagEntry : etagList) {
                if (!dumpService.isExistsByEtag(etagEntry)) {
                    throw new JobParametersInvalidException("invalid etag entry: " +
                            etagEntry + ". please check etag again.");
                }
            }

            List<DumpItem> dumpList = Arrays.stream(etagList)
                    .map(dumpService::getDumpByEtag)
                    .collect(Collectors.toList());

            for (DumpType value : DumpType.values()) {
                int count = 0;
                DumpItem initial = null;
                for (DumpItem dump : dumpList) {
                    if (dump.getDumpType().equals(value)) {
                        initial = dump;
                        count++;
                    }
                    if (count > 1) {
                        throw new JobParametersInvalidException(
                                String.format("duplicated dump entry detected for type: %s. etag: {%s}, {%s}",
                                        value.name(),
                                        initial.getETag(),
                                        dump.getETag()));
                    }
                }
            }
        }

        // check and validate date
        if (date != null) {
            if (date.matches("^\\d{4}-\\d{1,2}-\\d{1,2}$")) {
                throw new JobParametersInvalidException("yearMonth cannot contain date value. expected format: yyyy-mm");
            }
            if (!date.matches("^\\d{4}-\\d{1,2}$")) {
                throw new JobParametersInvalidException("invalid date format: {" + date + "}. expected format: yyyy-mm");
            }
        }

        // check type is presented properly
        if (type != null && (type.isBlank() || type.split("[,]").length < 1)) {
            throw new JobParametersInvalidException("type parameter cannot be empty");
        }
    }

    private String getString(JobParameters jobParameters, String key) {
        JobParameter jobParameter = extractJobParameter(jobParameters, key);
        if (jobParameter == null) {
            return null;
        }
        return String.valueOf(jobParameter.getValue());
    }

    private JobParameter extractJobParameter(JobParameters parameters, String key) {
        return parameters.getParameters().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(key.trim()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private Long getLong(JobParameters jobParameters, String key) {
        JobParameter jobParameter = extractJobParameter(jobParameters, key);
        if (jobParameter == null || (jobParameter.getType() != JobParameter.ParameterType.LONG)) {
            return null;
        }
        return (Long) jobParameter.getValue();
    }
}
