package io.dsub.discogs.batch;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO: FINISH REFERENCE Liquibase migration for db schema check or initialization from common pkg.
 * Note that the specified changelog has nothing to do with batch infrastructure specific tables
 * such as job execution or step execution metadata.
 * <p>
 * For initialization of spring batch tables, check {@link DataSourceInitializerConfig}.
 */
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