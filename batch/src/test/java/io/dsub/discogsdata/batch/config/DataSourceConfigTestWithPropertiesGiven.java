package io.dsub.discogsdata.batch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import javax.sql.DataSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataSourceConfigTestWithPropertiesGiven {
  @Mock ApplicationArguments applicationArguments;

  @InjectMocks private DataSourceConfig dataSourceConfig;

  @Test
  void shouldProvideDataSource() {
    DataSource dataSource = dataSourceConfig.batchDataSource();
    verify(applicationArguments).getNonOptionArgs();
    assertThat(dataSource).isNotNull();
  }

  @Test
  void getApplicationArgumentsShouldNotReturnNull() {
    assertThat(dataSourceConfig.getApplicationArguments()).isNotNull();
  }
}
