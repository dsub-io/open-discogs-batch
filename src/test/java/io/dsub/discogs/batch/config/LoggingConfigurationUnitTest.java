package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.logging.LogFile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

class LoggingConfigurationUnitTest {

  private static final String APPLICATION_CONFIGURATION = "application.yml";
  private static final String APPLICATION_PROPERTY_SOURCE = "application-configuration";
  private static final String FILE_NAME_ENVIRONMENT_VARIABLE = "LOGGING_FILE_NAME";
  private static final String ENVIRONMENT_PROPERTY_SOURCE = "logging-environment";
  private static final String OPT_IN_LOG_FILE = "/var/log/open-discogs/batch.log";

  @Test
  void defaultConfigurationUsesConsoleLoggingOnly() throws IOException {
    ConfigurableEnvironment environment = loadApplicationEnvironment();

    assertThat(environment.getProperty(LogFile.FILE_NAME_PROPERTY)).isNull();
    assertThat(environment.getProperty(LogFile.FILE_PATH_PROPERTY)).isNull();
    assertThat(LogFile.get(environment)).isNull();
  }

  @Test
  void standardEnvironmentVariableCanOptInToFileLogging() {
    ConfigurableEnvironment environment = new MockEnvironment();
    PropertySource<Map<String, Object>> environmentVariables =
        new SystemEnvironmentPropertySource(
            ENVIRONMENT_PROPERTY_SOURCE,
            Map.of(FILE_NAME_ENVIRONMENT_VARIABLE, OPT_IN_LOG_FILE));
    environment.getPropertySources().addFirst(environmentVariables);

    assertThat(environment.getProperty(LogFile.FILE_NAME_PROPERTY)).isEqualTo(OPT_IN_LOG_FILE);
    assertThat(LogFile.get(environment)).hasToString(OPT_IN_LOG_FILE);
  }

  private static ConfigurableEnvironment loadApplicationEnvironment() throws IOException {
    Resource applicationConfiguration = new ClassPathResource(APPLICATION_CONFIGURATION);
    List<PropertySource<?>> propertySources =
        new YamlPropertySourceLoader()
            .load(APPLICATION_PROPERTY_SOURCE, applicationConfiguration);
    ConfigurableEnvironment environment = new MockEnvironment();
    propertySources.forEach(environment.getPropertySources()::addLast);
    return environment;
  }
}
