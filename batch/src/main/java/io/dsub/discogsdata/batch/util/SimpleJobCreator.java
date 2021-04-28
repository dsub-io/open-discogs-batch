package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.config.AppConfig;
import io.dsub.discogsdata.common.exception.MissingRequiredParamsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleJobCreator implements JobCreator {

    private static final String ARTIST = "artist";
    private static final String LABEL = "label";
    private static final String MASTER = "master";
    private static final String RELEASE = "release";

    private static final List<String> KEYS = List.of(ARTIST, LABEL, MASTER, RELEASE);

    private final Step artistStep;
    private final Step labelStep;
    private final Step masterStep;
    private final Step releaseItemStep;

    private final DiscogsJobParametersValidator validator;
    private final JobBuilderFactory jbf;

    @Override
    public Job make(JobParameters jobParameters) {
        assert (jobParameters != null);

        Map<String, JobParameter> parameterMap = jobParameters.getParameters().entrySet().stream()
                .filter(entry -> KEYS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("generating new job based on {}", parameterMap);

        SimpleJobBuilder simpleJobBuilder = jbf.get(AppConfig.JOB_NAME)
                .validator(validator)
                .listener(getJobExecutionListener())
                .start(getNextStep(parameterMap));

        while (parameterMap.size() > 0) {
            simpleJobBuilder = simpleJobBuilder.next(getNextStep(parameterMap));
        }

        return simpleJobBuilder.build();
    }

    private Step getNextStep(Map<String, JobParameter> parameterMap) {

        Step found = null;

        for (String key : KEYS) {
            if (parameterMap.containsKey(key)) {
                parameterMap.remove(key);
                found = getFlowByName(key);
                break;
            }
        }

        if (found == null) {
            throw new MissingRequiredParamsException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "error while parsing key from jobParameters");
        }

        return found;
    }

    private Step getFlowByName(String name) {
        switch (name) {
            case ARTIST: return artistStep;
            case LABEL: return labelStep;
            case MASTER: return masterStep;
            case RELEASE: return releaseItemStep;
        }
        return null;
    }

    private JobExecutionListener getJobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {}
            @Override
            public void afterJob(JobExecution jobExecution) {
                System.exit(0);
            }
        };
    }
}
