package io.dsub.discogsdata.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.dsub.discogsdata.batch.datasource.DBType;
import io.dsub.discogsdata.batch.datasource.DataSourceConfig;
import io.dsub.discogsdata.batch.query.QueryBuilder;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.mockito.junit.MockitoRule;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@Slf4j
class QueryBuilderConfigTest {

  static String DB_TYPE_FIELD_NAME = "DB_TYPE";
  static DBType DEFAULT_DB_TYPE = DataSourceConfig.getDB_TYPE();

  ApplicationContextRunner ctx = new ApplicationContextRunner()
      .withUserConfiguration(QueryBuilderConfig.class);


  @BeforeEach
  void setUp() {

  }

  @AfterEach
  void cleanUp() {
    setDBTypeForDataSourceConfig(DEFAULT_DB_TYPE);
  }

  @Test
  void whenDBTypeIsNull__ShouldThrow() {
    setDBTypeForDataSourceConfig(null);
    ctx.run(it -> {
      assertThat(it).hasFailed();
      assertThat(it).getFailure().getRootCause().hasMessage("DB_TYPE from DataSourceConfig.class cannot be null");
      assertThat(it).getFailure().getRootCause().isInstanceOf(InitializationFailureException.class);
    });
  }

  @ParameterizedTest
  @EnumSource(DBType.class)
  void givenDBTypeIsPostgresQL__ShouldHaveQueryBuilderBean(DBType type) {
    setDBTypeForDataSourceConfig(type);
    ctx.run(it -> assertThat(it).hasSingleBean(QueryBuilder.class));
  }

  private void setDBTypeForDataSourceConfig(DBType type) {
    try {
      Field dbTypeField = DataSourceConfig.class.getDeclaredField(DB_TYPE_FIELD_NAME);
      dbTypeField.setAccessible(true);
      dbTypeField.set(DataSourceConfig.class, type);
    } catch (NoSuchFieldException e) {
      log.error("failed to locate DB_TYPE_FIELD_NAME on DataSourceConfig.class");
      fail(e);
    } catch (IllegalAccessException e) {
      log.error("failed to set DB_TYPE for DataSourceConfig.class");
      fail(e);
    }
  }
}