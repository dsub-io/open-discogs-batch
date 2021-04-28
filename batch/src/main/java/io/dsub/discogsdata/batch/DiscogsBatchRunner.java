package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.util.JobCreator;
import io.dsub.discogsdata.batch.util.SimpleDiscogsBatchJobParameterResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Order(2)
@Component
@RequiredArgsConstructor
public class DiscogsBatchRunner implements ApplicationRunner {

    private final JobParametersConverter converter = new DefaultJobParametersConverter();
    private final JobLauncher jobLauncher;
    private final JobCreator jobCreator;
    private final SimpleDiscogsBatchJobParameterResolver jobParameterResolver;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        JobParameters params = makeJobParameters(args);
        jobLauncher.run(jobCreator.make(params), params);
    }

    private JobParameters makeJobParameters(ApplicationArguments args) {
        String[] jobArguments = args.getNonOptionArgs().toArray(new String[0]);
        Properties props = StringUtils.splitArrayElementsIntoProperties(jobArguments, "=");
        JobParameters parameters = converter.getJobParameters(props);
        return jobParameterResolver.resolve(parameters);
    }
}
