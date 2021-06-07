package io.dsub.discogs.batch.util;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.common.exception.InvalidArgumentException;
import io.dsub.discogs.common.exception.MissingRequiredArgumentException;
import java.util.List;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.integration.support.PropertiesBuilder;
import org.springframework.stereotype.Component;

@Component
public class DiscogsJobParametersConverter implements JobParametersConverter, InitializingBean {

  public static final String DOUBLE = "(double)";
  public static final String LONG = "(long)";
  public static final String STRING = "(string)";

  @Getter
  @Setter
  private JobParametersConverter delegate;

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

    for (String optionName : args.getOptionNames()) { // option arguments for the beginning...

      List<String> optionValues = args.getOptionValues(optionName);
      String options = String.join(",", optionValues); // join as multi values
      ArgType argType = ArgType.getTypeOf(optionName);

      if (argType == null) { // throws if argType not found
        throw new InvalidArgumentException(
            "failed to identify the argument type for entry: " + optionName);
      }

      // procedures to set appropriate type bracket
      optionName = argType.getGlobalName();
      Class<?> supportedType = ArgType.getTypeOf(optionName).getSupportedType();
      optionName = appendTypeBracket(optionName, supportedType);
      // conclusion
      builder.put(optionName, options);
    }
    for (String nonOptionArg : args.getNonOptionArgs()) {
      // find the first index of the given delimiter.
      int idx = nonOptionArg.indexOf('=');
      // parsing the name...
      String name;
      if (idx == -1) { // if not found, name is the entry as a whole.
        name = nonOptionArg;
      } else {
        name = nonOptionArg.substring(0, idx); // found, hence proceed..
      }

      ArgType argType = ArgType.getTypeOf(name);
      if (argType == null) { // argType not found for given name...
        throw new InvalidArgumentException(
            "failed to identify the argument type for entry: " + name);
      }

      // set name as globalName so that it could be handled with confidence
      name = argType.getGlobalName();
      Class<?> supportedType = argType.getSupportedType();

      // value parsing...
      String value;
      if (idx == nonOptionArg.length() - 1) { // meaning '=' was at the end of the entry.
        // must be thrown as it indicates we expect any sort of value mapped to it.
        throw new InvalidArgumentException(
            "found mapping mark('=') for " + name + ", but the value is missing.");
      } else if (idx == -1) { // '=' was not found; we can consider this as an empty string.
        value = "";
      } else {
        value = nonOptionArg.substring(idx + 1); // parse value as usual (key=value) pair.
      }
      // append bracket to the name of the argument.
      name = appendTypeBracket(name, supportedType);
      builder.put(name, value);
    }

    // finalize the procedure by delegation.
    return delegate.getJobParameters(builder.get());
  }

  /**
   * Appends appropriate type indication for each entry name. Currently, if supported class is other
   * than long or double, it will be simply considered to be used as a string.
   *
   * @param name          name of the argument
   * @param supportedType type of the argument
   * @return name as the type appended to
   */
  public String appendTypeBracket(String name, Class<?> supportedType) {
    if (supportedType.equals(Long.class)) {
      name += LONG;
    } else if (supportedType.equals(Double.class)) {
      name += DOUBLE;
    } else {
      name += STRING;
    }
    return name;
  }

  /**
   * Method to fulfill the {@link InitializingBean#afterPropertiesSet()}.
   *
   * @throws MissingRequiredArgumentException thrown if delegation is set to null;
   */
  @Override
  public void afterPropertiesSet() throws MissingRequiredArgumentException {
    if (this.delegate == null) {
      throw new MissingRequiredArgumentException("delegate should not be null");
    }
  }

  /**
   * {@link JobParametersConverter#getJobParameters(Properties)} to be delegated to its delegate.
   *
   * @param properties properties to be extracted.
   * @return formatted {@link JobParameters}.
   */
  @Override
  public JobParameters getJobParameters(Properties properties) {
    return delegate.getJobParameters(properties);
  }

  /**
   * {@link JobParametersConverter#getProperties(JobParameters)} to be delegated to its delegate.
   *
   * @param params job parameters to be extracted.
   * @return formatted {@link Properties}.
   */
  @Override
  public Properties getProperties(JobParameters params) {
    return delegate.getProperties(params);
  }
}
