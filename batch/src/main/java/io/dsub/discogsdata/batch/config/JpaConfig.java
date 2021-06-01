package io.dsub.discogsdata.batch.config;

import java.util.Objects;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
@EnableTransactionManagement
@EntityScan(basePackages = {
    "io.dsub.discogsdata.common.entity",
    "io.dsub.discogsdata.batch.dump"})
@EnableJpaRepositories(basePackages = {
    "io.dsub.discogsdata.common.repository",
    "io.dsub.discogsdata.batch.dump"})
public class JpaConfig {

  private final DataSource dataSource;
  private final JpaVendorAdapter jpaVendorAdapter;

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
    emf.setDataSource(dataSource);
    emf.setJpaVendorAdapter(jpaVendorAdapter);
    emf.setPackagesToScan(
        "io.dsub.discogsdata.common",
        "io.dsub.discogsdata.batch");
    emf.setJpaDialect(new HibernateJpaDialect());
    Properties properties = new Properties();
    properties.setProperty("hibernate.format_sql", "true");
    properties.setProperty("hibernate.order_inserts", "true");
    properties.setProperty("hibernate.order_updates", "true");
    properties.setProperty("hibernate.jdbc.batch_size", "20000");
    properties.setProperty("hibernate.jdbc.time_zone", "UTC");
    emf.setJpaProperties(properties);
    emf.afterPropertiesSet();
    return emf;
  }

  @Bean(name = "batchTransactionManager")
  public PlatformTransactionManager transactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager(
        Objects.requireNonNull(entityManagerFactory().getObject()));
    transactionManager.setDataSource(dataSource);
    transactionManager.setValidateExistingTransaction(true);
    transactionManager.setRollbackOnCommitFailure(true);
    transactionManager.setNestedTransactionAllowed(true);
    return transactionManager;
  }
}