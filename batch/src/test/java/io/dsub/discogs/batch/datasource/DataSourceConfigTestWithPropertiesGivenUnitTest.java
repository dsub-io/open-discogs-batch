package io.dsub.discogs.batch.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.common.exception.InitializationFailureException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

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

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  ApplicationArguments applicationArguments;

  private DataSourceConfig dataSourceConfig;

  @BeforeEach
  void setUp() {
    applicationArguments = mock(ApplicationArguments.class);
    dataSourceConfig = new DataSourceConfig(applicationArguments);
    dataSourceConfig = Mockito.spy(dataSourceConfig);
  }

  @Test
  void whenBatchDataSourceCalled__ShouldCallDataSourceProperties__ThenReturnDataSource() {
    DataSourceProperties dataSourceProperties = Mockito.mock(DataSourceProperties.class);
    doReturn(dataSourceProperties).when(dataSourceConfig).dataSourceProperties();
    doReturn("test").when(dataSourceProperties).getUsername();
    doReturn("test").when(dataSourceProperties).getPassword();
    doReturn("test").when(dataSourceProperties).getConnectionUrl();
    doReturn(DBType.MYSQL).when(dataSourceProperties).getDbType();
    doNothing().when(dataSourceConfig).initializeSchema(any());

    // when
    DataSource dataSource = dataSourceConfig.batchDataSource();

    // then
    verify(dataSourceConfig, atLeast(1)).dataSourceProperties();
    assertThat(dataSource).isNotNull();
  }

  @Test
  void whenInvalidJdbcUrlPresentedInOptions__THenShouldThrowMissingRequiredArgumentException() {
    // when
    Throwable t = catchThrowable(() -> dataSourceConfig.batchDataSource());

    // then
    assertThat(t).hasMessage(MISSING_REQUIRED_ARG_MESSAGE);
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
  void givenApplicationArgs__WhenDataSourceProperties__ShouldContainAllData() {

    // given
    doReturn(List.of(PLAIN_TEST_JDBC_URL_ARGUMENT, "username=user", "password=pass"))
        .when(applicationArguments)
        .getNonOptionArgs();

    // when
    DataSourceProperties properties = dataSourceConfig.dataSourceProperties();

    // then
    assertAll(
        () -> assertThat(properties.getConnectionUrl()).contains(PLAIN_TEST_JDBC_URL_VALUE),
        () -> assertThat(properties.getDbType()).isEqualTo(DBType.MYSQL),
        () -> assertThat(properties.getPassword()).isEqualTo("pass"),
        () -> assertThat(properties.getUsername()).isEqualTo("user"));
  }

  @Test
  void whenInitializeSchema__ResourceThrows__ThenShouldCatchThrow() throws IOException {
    // given
    DataSource dataSource = mock(DataSource.class);
    Resource resource = mock(Resource.class);
    doReturn(resource).when(dataSourceConfig).getSchemaResource();
    doThrow(new IOException("EXCEPTION")).when(resource).getInputStream();

    // when
    Throwable t = catchThrowable(() -> dataSourceConfig.initializeSchema(dataSource));

    // then
    assertAll(
        () -> assertThat(t).hasMessage("EXCEPTION")
            .isInstanceOf(InitializationFailureException.class),
        () -> assertThat(logSpy.getLogsByLevelAsString(Level.ERROR, true))
            .hasSize(1)
            .contains("failed to initialize schema: EXCEPTION"));
  }

  @Test
  void givenSqlStrings__WhenInitializeSchema__ShouldCallJdbcTemplates() {
    // given
    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    DataSource dataSource = Mockito.mock(DataSource.class);

    Resource resource = new ByteArrayResource("hello world;\nHi there!;".getBytes());

    doReturn(jdbcTemplate).when(dataSourceConfig).getJdbcTemplate(any());
    doReturn(resource).when(dataSourceConfig).getSchemaResource();
    doNothing().when(jdbcTemplate).execute(captor.capture());

    // when
    dataSourceConfig.initializeSchema(dataSource);

    // then
    assertAll(
        () -> verify(jdbcTemplate, atLeast(2)).execute(anyString()),
        () -> assertThat(captor.getAllValues())
            .hasSize(2)
            .contains("hello world", "Hi there!"));
  }

  @Test
  void whenGetSchemaResource__ShouldReturnValidValue() {
    DataSourceProperties dataSourceProperties = Mockito.mock(DataSourceProperties.class);
    doReturn(dataSourceProperties).when(dataSourceConfig).dataSourceProperties();

    // given
    doReturn(DBType.MYSQL).when(dataSourceProperties).getDbType();

    // when
    Resource firstResult = dataSourceConfig.getSchemaResource();

    // then
    assertThat(firstResult).isEqualTo(dataSourceConfig.getMysqlSchema());

    // prep
    doReturn(DBType.POSTGRESQL).when(dataSourceProperties).getDbType();

    // when
    Resource secondResult = dataSourceConfig.getSchemaResource();

    // then
    assertThat(secondResult).isEqualTo(dataSourceConfig.getPostgresSchema());
  }
}
