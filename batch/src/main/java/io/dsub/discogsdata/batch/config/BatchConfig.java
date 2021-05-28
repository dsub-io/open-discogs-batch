package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.datasource.DataSourceConfig;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@EnableAsync
@Configuration
@EnableBatchProcessing
@Import({DataSourceConfig.class, JpaConfig.class})
@RequiredArgsConstructor
public class BatchConfig implements BatchConfigurer {

  public static final int CORE_SIZE = Runtime.getRuntime().availableProcessors();
  public static final int DEFAULT_CHUNK_SIZE = 1000;
  public static final int DEFAULT_THROTTLE_LIMIT = CORE_SIZE > 2 ? (int) (CORE_SIZE * 0.8) : 1;

  private final PlatformTransactionManager batchTransactionManager;
  private final DataSource dataSource;

  /**
   * Primary bean for batch processing. The core and max pool size is fit into same value, just as
   * the same as the core size of host processor count.
   *
   * <p>{@link ThreadPoolTaskExecutor#setWaitForTasksToCompleteOnShutdown(boolean)}} is set to
   * true, in case of additional tasks required after the actual job is doe.
   *
   * @return instance of {@link ThreadPoolTaskExecutor}.
   */

  @Bean
  public ThreadPoolTaskExecutor batchTaskExecutor() {
    ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(CORE_SIZE);
    taskExecutor.setMaxPoolSize(CORE_SIZE);
    taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
    taskExecutor.afterPropertiesSet();
    taskExecutor.initialize();
    return taskExecutor;
  }

  @Override
  public JobRepository getJobRepository() throws Exception {
    JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
    jobRepositoryFactoryBean.setTransactionManager(getTransactionManager());
    jobRepositoryFactoryBean.setDataSource(dataSource);
    jobRepositoryFactoryBean.afterPropertiesSet();
    return jobRepositoryFactoryBean.getObject();
  }

  @Override
  public PlatformTransactionManager getTransactionManager() {
    return batchTransactionManager;
  }

  @Override
  public JobLauncher getJobLauncher() throws Exception {
    SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
    jobLauncher.setJobRepository(getJobRepository());
    jobLauncher.setTaskExecutor(batchTaskExecutor());
    jobLauncher.afterPropertiesSet();
    return jobLauncher;
  }

  @Override
  public JobExplorer getJobExplorer() throws Exception {
    JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
    jobExplorerFactoryBean.setDataSource(dataSource);
    jobExplorerFactoryBean.afterPropertiesSet();
    return jobExplorerFactoryBean.getObject();
  }
}