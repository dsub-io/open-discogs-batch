package io.dsub.discogsdata.batch.job;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.integration.support.PropertiesBuilder;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class DiscogsJobParametersConverter implements JobParametersConverter {

    public static final String DOUBLE = "(double)";
    public static final String LONG = "(long)";
    public static final String STRING = "(string)";
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("\\d*\\.\\d*");
    private static final Pattern LONG_PATTERN = Pattern.compile("\\d+");
    private final JobParametersConverter delegate = new DefaultJobParametersConverter();
    private JobParametersValidator validator;

    public JobParameters getJobParameters(ApplicationArguments args) {
        PropertiesBuilder builder = new PropertiesBuilder();
        for (String optionName : args.getOptionNames()) {
            List<String> optionValues = args.getOptionValues(optionName);
            String options = String.join(",", optionValues);
            builder.put(optionName, options);
        }
        return delegate.getJobParameters(builder.get());
    }

    @Override
    public JobParameters getJobParameters(Properties properties) {
        return delegate.getJobParameters(properties);
    }

    @Override
    public Properties getProperties(JobParameters params) {
        return delegate.getProperties(params);
    }
}
