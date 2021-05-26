package io.dsub.discogsdata.batch.datasource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogsdata.batch.datasource.DataSourceConfig;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

@ExtendWith(MockitoExtension.class)
class DataSourceConfigTestWithPropertiesGivenUnitTest {

  private static final String MISSING_REQUIRED_ARG_MESSAGE =
      "application argument must contain url in the non-option argument.";

  private static final String TIME_ZONE_UTC_OPT = "serverTimeZone=UTC";
  private static final String CACHE_PREP_STMT_OPT = "cachePrepStmts=true";
  private static final String USE_SERVER_PREP_STMTS_OPT = "useServerPrepStmts=true";
  private static final String REWRITE_BATCHED_STMTS_OPT = "rewriteBatchedStatements=true";

  private static final List<String> OPTION_LIST =
      List.of(
          TIME_ZONE_UTC_OPT,
          CACHE_PREP_STMT_OPT,
          USE_SERVER_PREP_STMTS_OPT,
          REWRITE_BATCHED_STMTS_OPT);

  private static final String PLAIN_TEST_JDBC_URL_VALUE =
      "jdbc:mysql://hello.world.com:3306/somewhere";
  private static final String PLAIN_TEST_JDBC_URL_ARGUMENT = "url=" + PLAIN_TEST_JDBC_URL_VALUE;

  @Mock
  ApplicationArguments applicationArguments;

  @InjectMocks
  private DataSourceConfig dataSourceConfig;

  @Test
  void whenBatchDataSourceCalled__ShouldCallApplicationArguments__GetNonOptionArg() {

    // when
    when(applicationArguments.getNonOptionArgs()).thenReturn(List.of(PLAIN_TEST_JDBC_URL_ARGUMENT));
    dataSourceConfig.batchDataSource();

    // then
    verify(applicationArguments, atLeast(1)).getNonOptionArgs();
  }

  @Test
  void whenValidJdbcUrlPresentedInOptions__ThenShouldReturnNonNullDataSourceInstance() {
    // when
    when(applicationArguments.getNonOptionArgs()).thenReturn(List.of(PLAIN_TEST_JDBC_URL_ARGUMENT));
    DataSource dataSource = dataSourceConfig.batchDataSource();

    // then
    assertThat(dataSource).isNotNull();
  }

  @Test
  void whenInvalidJdbcUrlPresentedInOptions__THenShouldThrowMissingRequiredArgumentException() {
    // when
    when(applicationArguments.getNonOptionArgs()).thenReturn(Collections.emptyList());

    // then
    assertThatThrownBy(() -> dataSourceConfig.batchDataSource())
        .hasMessage(MISSING_REQUIRED_ARG_MESSAGE);
  }

  @Test
  void whenMissingEntireOptions__ThenShouldIncludeAllRequiredArguments() {
    // when
    String result = dataSourceConfig.appendOptions(PLAIN_TEST_JDBC_URL_VALUE);

    // then
    assertThat(result)
        .contains(TIME_ZONE_UTC_OPT)
        .contains(REWRITE_BATCHED_STMTS_OPT)
        .contains(USE_SERVER_PREP_STMTS_OPT)
        .contains(CACHE_PREP_STMT_OPT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
          TIME_ZONE_UTC_OPT,
          REWRITE_BATCHED_STMTS_OPT,
          USE_SERVER_PREP_STMTS_OPT,
          CACHE_PREP_STMT_OPT
      })
  void whenMissingSingleOption__ThenShouldIncludeThatMissingArgument(String opt) {
    String options =
        OPTION_LIST.stream().filter(string -> !string.equals(opt)).collect(Collectors.joining("&"));
    String jdbcUrlString = PLAIN_TEST_JDBC_URL_VALUE + "?" + options;

    // when
    String result = dataSourceConfig.appendOptions(jdbcUrlString);
    assertThat(result).contains("&" + opt);
  }

  @Test
  void getApplicationArgumentsShouldNotReturnNull() {
    assertThat(dataSourceConfig.getApplicationArguments()).isNotNull();
  }
}
