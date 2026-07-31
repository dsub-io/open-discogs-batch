package io.dsub.discogs.batch.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class DiscogsJobParametersConverterUnitTest {

  final JobParametersConverter delegate = new DefaultJobParametersConverter();
  final DiscogsJobParametersConverter converter = new DiscogsJobParametersConverter(delegate);

  @Test
  void getJobParametersBy__ApplicationArguments__ShouldHaveProperValueMapped()
      throws InvalidArgumentException {
    String[] args =
        "url=localhost user=root pass=pass --entities=artist,label,master,release --chunk-size=1000"
            .split(" ");
    ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
    JobParameters jobParameters = converter.getJobParameters(applicationArguments);
    assertDoesNotThrow(() -> jobParameters.getLong("chunkSize"));
    assertThat(jobParameters.getString("url")).isEqualTo("localhost");
    assertThat(jobParameters.getString("username")).isEqualTo("root");
    assertThat(jobParameters.getString("password")).isEqualTo("pass");
    assertThat(jobParameters.getString("entities")).isEqualTo("artist,label,master,release");
    assertThat(jobParameters.getLong("chunkSize")).isEqualTo(1000L);
  }

  @Test
  void afterPropertiesSetMethodShouldThrowIfDelegateIsNull() {
    assertThatThrownBy(() -> new DiscogsJobParametersConverter(null).afterPropertiesSet())
        .hasMessage("delegate should not be null");
  }

  @ParameterizedTest
  @ValueSource(classes = {String.class, Double.class, Long.class})
  void appendTypeBracketMethodShouldAppendTypesProperly(Class<?> clazz) {
    List<String> names = List.of("a", "b", "c");
    names.forEach(
        name -> {
          String result = converter.appendTypeBracket(name, clazz);
          String expected = name + "(" + clazz.getSimpleName().toLowerCase(Locale.ROOT) + ")";
          assertThat(result).isEqualTo(expected);
        });
  }

  @Test
  void testGetterAndSetterForDelegate() {
    JobParametersConverter delegate = new DefaultJobParametersConverter();
    DiscogsJobParametersConverter converter = new DiscogsJobParametersConverter();

    converter.setDelegate(delegate); // other instance
    assertThat(converter.getDelegate()).isEqualTo(delegate);

    converter.setDelegate(this.delegate); // the instance
    assertThat(converter.getDelegate()).isNotEqualTo(delegate);

    converter.setDelegate(null);
    assertThat(converter.getDelegate()).isNull();
  }

  @Test
  void eachDelegatedMethodsShouldResultSameAsItsDelegate() {
    JobParameters parameters =
        new JobParametersBuilder()
            .addString("url", "localhost")
            .addString("username", "root")
            .addString("password", "pass")
            .addString("type", "artist")
            .addLong("chunkSize", 1000L)
            .toJobParameters();
    Properties properties = delegate.getProperties(parameters);
    assertThat(converter.getJobParameters(properties))
        .isEqualTo(delegate.getJobParameters(properties)); // vice
    JobParameters jobParameters = delegate.getJobParameters(properties);
    assertThat(converter.getProperties(jobParameters))
        .isEqualTo(delegate.getProperties(jobParameters)); // versa
  }
}
