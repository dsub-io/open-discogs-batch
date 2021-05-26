package io.dsub.discogsdata.batch.datasource;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DBTypeTest {

  @ParameterizedTest
  @EnumSource(DBType.class)
  void whenGetDriverClassName__ShouldReturnNotBlank(DBType dbType) {
    assertThat(dbType.getDriverClassName())
        .isNotBlank()
        .hasSizeGreaterThan(0);
  }

  @Test
  void whenGetDriverClassName__ShouldReturnMatchingDriverName() {
    // when
    String name = DBType.getDriverClassName("jdbc:mysql://");
    // then
    assertThat(name).isEqualTo("com.mysql.cj.jdbc.Driver");
    // when
    name = DBType.getDriverClassName("jdbc:postgresql://");
    // then
    assertThat(name).isEqualTo("org.postgresql.Driver");
  }

  @Test
  void whenGetNames__ShouldReturnLowerCases() {
    // when
    List<String> names = DBType.getNames();

    // then
    names.forEach(name -> assertThat(name)
        .isNotBlank()
        .isLowerCase());
  }
}