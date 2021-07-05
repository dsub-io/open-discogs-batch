package io.dsub.discogs.batch.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DBTypeTest {

  @EnumSource(value = DBType.class)
  @ParameterizedTest
  void whenGetDriverClassName__ShouldNotReturnNullOrBlank(DBType dbType) {
    // when
    String driverClassName = dbType.getDriverClassName();

    // then
    assertThat(driverClassName)
        .isNotNull()
        .isNotBlank();
  }

  @Test
  void whenGetNames__ShouldReturnLowerCases() {
    // when
    List<String> names = DBType.getNames();

    // then
    names.forEach(name -> assertThat(name).isNotBlank().isLowerCase());
  }
}