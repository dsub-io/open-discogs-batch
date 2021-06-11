package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.datasource.SimpleDataSourceProperties;
import io.dsub.discogs.batch.query.QueryBuilder;
import io.dsub.discogs.common.exception.InvalidArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@Slf4j
class QueryBuilderConfigTest {

  ApplicationContextRunner ctx;

  @BeforeEach
  void setUp() {
    ctx = new ApplicationContextRunner().withUserConfiguration(QueryBuilderConfig.class);
  }

  @Test
  void whenDBTypeIsNull__ShouldThrow() {
    ctx =
        ctx.withBean(
            SimpleDataSourceProperties.class, new DefaultApplicationArguments("url=jdbc:h2:"));

    ctx.run(
        it -> {
          assertThat(it).hasFailed();
          assertThat(it)
              .getFailure()
              .getRootCause()
              .hasMessage("failed to recognize DB type from jdbc:h2:");
          assertThat(it).getFailure().getRootCause().isInstanceOf(InvalidArgumentException.class);
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"url=jdbc:mysql://", "url=jdbc:postgresql"})
  void givenDBTypeIsPostgresQL__ShouldHaveQueryBuilderBean(String jdbcUrl) {
    ctx.withBean(SimpleDataSourceProperties.class, new DefaultApplicationArguments(jdbcUrl))
        .run(it -> assertThat(it).hasSingleBean(QueryBuilder.class));
  }
}
