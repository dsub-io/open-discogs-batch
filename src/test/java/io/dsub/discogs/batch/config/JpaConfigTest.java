package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.testutil.LogSpy;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@DataJpaTest
class JpaConfigTest {

  ApplicationContextRunner ctx;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @Autowired DataSource dataSource;

  @Autowired JpaVendorAdapter jpaVendorAdapter;

  @Autowired LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean;

  @BeforeEach
  void setUp() {
    ctx =
        new ApplicationContextRunner()
            .withUserConfiguration(JpaConfig.class)
            .withBean(DataSource.class, () -> dataSource)
            .withBean(JpaVendorAdapter.class, () -> jpaVendorAdapter);
  }

  @Test
  void whenCtxRun__ShouldReturnRequiredBeans() {
    // when
    ctx.run(
        it -> {
          // then
          assertThat(localContainerEntityManagerFactoryBean.getDataSource()).isEqualTo(dataSource);
          assertThat(localContainerEntityManagerFactoryBean.getJpaPropertyMap())
              .containsEntry("hibernate.format_sql", "true")
              .containsEntry("hibernate.order_inserts", "true")
              .containsEntry("hibernate.order_updates", "true")
              .containsEntry("hibernate.jdbc.batch_size", "20000")
              .containsEntry("hibernate.jdbc.time_zone", "UTC");
        });
  }
}
