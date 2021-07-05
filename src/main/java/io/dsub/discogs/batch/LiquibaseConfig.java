package io.dsub.discogs.batch;

import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Liquibase Changelog
 **/
@Slf4j
@Configuration
public class LiquibaseConfig {
  @Bean
  public SpringLiquibase liquibase(DataSource dataSource) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setChangeLog("db/changelog/db.changelog-master.yaml");
    liquibase.setShouldRun(true);
    liquibase.setDataSource(dataSource);
    return liquibase;
  }
}