package io.dsub.discogs.batch.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
@RequiredArgsConstructor
public class DiscogsBatchConfigurer implements BatchConfigurer {

  private final DataSource dataSource;
  private final ThreadPoolTaskExecutor taskExecutor;

  @Override
  public JobRepository getJobRepository() throws Exception {
    JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    factory.setTransactionManager(getTransactionManager());
    factory.setDataSource(dataSource);
    factory.afterPropertiesSet();
    return factory.getObject();
  }

  /**
   * {@link PlatformTransactionManager} to be used for spring batch context.
   *
   * @return a new, separated transaction manager from the business logics.
   */
  @Override
  public PlatformTransactionManager getTransactionManager() {
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
    transactionManager.setDataSource(dataSource);
    transactionManager.afterPropertiesSet();
    return transactionManager;
  }

  @Override
  public JobLauncher getJobLauncher() throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setTaskExecutor(taskExecutor);
    jobLauncher.setJobRepository(getJobRepository());
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  @Override
  public JobExplorer getJobExplorer() throws Exception {
    JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
    factory.setDataSource(dataSource);
    factory.afterPropertiesSet();
    return factory.getObject();
  }
}
