package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.argument.ArgType;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.integration.support.PropertiesBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

@Component
public class DiscogsJobParametersConverter implements JobParametersConverter, InitializingBean {

  public static final String DOUBLE = "(double)";
  public static final String LONG = "(long)";
  public static final String STRING = "(string)";

  private final JobParametersConverter delegate;

  public DiscogsJobParametersConverter() {
    this.delegate = new DefaultJobParametersConverter();
  }

  public DiscogsJobParametersConverter(JobParametersConverter delegate) {
    this.delegate = delegate;
  }

  /**
   * Appends name of the supported value type accordingly. Also, it will separate the values by
   * delimiter ','.
   *
   * @param args arguments to be parsed into {@link JobParameters}.
   * @return parsed result.
   */
  public JobParameters getJobParameters(ApplicationArguments args) {
    PropertiesBuilder builder = new PropertiesBuilder();
    for (String optionName : args.getOptionNames()) {
      List<String> optionValues = args.getOptionValues(optionName);
      String options = String.join(",", optionValues);
      Class<?> supportedType = ArgType.getTypeOf(optionName).getSupportedType();
      if (supportedType.equals(Long.class)) {
        optionName += LONG;
      } else if (supportedType.equals(Double.class)) {
        optionName += DOUBLE;
      } else {
        optionName += STRING;
      }
      builder.put(optionName, options);
    }
    return delegate.getJobParameters(builder.get());
  }

  @Override
  public void afterPropertiesSet() {
    assert (this.delegate != null);
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
