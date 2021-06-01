package io.dsub.discogsdata.batch.datasource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogsdata.common.exception.InitializationFailureException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

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

  @BeforeEach
  void setUp() {
    dataSourceConfig = Mockito.spy(dataSourceConfig);
  }

  @AfterEach
  void reset() throws NoSuchFieldException, IllegalAccessException {
    Field dbTypeField = DataSourceConfig.class.getDeclaredField("DB_TYPE");
    dbTypeField.setAccessible(true);
    dbTypeField.set(DataSourceConfig.class, null);
  }

  @Test
  void whenBatchDataSourceCalled__ShouldCallApplicationArguments__GetNonOptionArg() {

    when(applicationArguments.getNonOptionArgs()).thenReturn(List.of(PLAIN_TEST_JDBC_URL_ARGUMENT));
    doNothing().when(dataSourceConfig).initializeSchema(any());

    // when
    dataSourceConfig.batchDataSource();

    // then
    verify(applicationArguments, atLeast(1)).getNonOptionArgs();
  }

  @Test
  void whenValidJdbcUrlPresentedInOptions__ThenShouldReturnNonNullDataSourceInstance() {
    // when
    when(applicationArguments.getNonOptionArgs()).thenReturn(List.of(PLAIN_TEST_JDBC_URL_ARGUMENT));
    doNothing().when(dataSourceConfig).initializeSchema(any());

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

  @Test
  void initializeSchema__ShouldThrowIfDBTypeIsNull() {
    DataSource dataSource = Mockito.mock(DataSource.class);
    Throwable t = catchThrowable(() -> dataSourceConfig.initializeSchema(dataSource));
    assertThat(t).hasMessage("static variable DB_TYPE cannot be null");
  }

  @Test
  void initializeSchema__ShouldLogWhenExceptionThrown()
      throws NoSuchFieldException, IllegalAccessException, SQLException {

    // prep
    DataSource dataSource = Mockito.mock(DataSource.class);
    Field dbTypeField = DataSourceConfig.class.getDeclaredField("DB_TYPE");
    dbTypeField.setAccessible(true);
    dbTypeField.set(DataSourceConfig.class, DBType.MYSQL);
    Field mysqlSchemaField = DataSourceConfig.class.getDeclaredField("mysqlSchema");
    mysqlSchemaField.setAccessible(true);
    ResourceLoader resourceLoader = new DefaultResourceLoader();
    mysqlSchemaField
        .set(dataSourceConfig, resourceLoader.getResource("classpath:mysql-test-schema.sql"));
    Connection conn = Mockito.mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(conn);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
    when(conn.prepareStatement(any())).thenReturn(preparedStatement);
    doThrow(new SQLException("test-exception")).when(conn).commit();

    // when
    Throwable t = catchThrowable(() -> dataSourceConfig.initializeSchema(dataSource));
    // then
    assertThat(t).hasMessage("test-exception");
  }

  @Test
  void whenGetSchemaResource__ShouldReturnValidValue() {
    try {
      // prep
      Field dbTypeField = DataSourceConfig.class.getDeclaredField("DB_TYPE");
      dbTypeField.setAccessible(true);
      dbTypeField.set(DataSourceConfig.class, DBType.MYSQL);

      // when
      Resource firstResult = dataSourceConfig.getSchemaResource();

      // then
      assertThat(firstResult).isEqualTo(dataSourceConfig.getMysqlSchema());

      // prep
      dbTypeField.set(DataSourceConfig.class, DBType.POSTGRESQL);

      // when
      Resource secondResult = dataSourceConfig.getSchemaResource();

      // then
      assertThat(secondResult).isEqualTo(dataSourceConfig.getPostgresSchema());

    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail(e.getMessage());
    }
  }

  @Test
  void whenGetSchemaResource__ShouldThrowIfNoDBTypeSet() {
    Throwable t = catchThrowable(() -> dataSourceConfig.getSchemaResource());
    assertThat(t).hasMessage("static variable DB_TYPE cannot be null");
  }
}
