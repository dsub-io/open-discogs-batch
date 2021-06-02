package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;

@EnableAsync
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig implements BatchConfigurer {

  public static final int DEFAULT_CHUNK_SIZE = 1000;

  public static final String JOB_NAME = "discogs-batch-job" + LocalDateTime.now();
  private static final String FAILED = "FAILED";
  private static final String ANY = "*";

  private final PlatformTransactionManager batchTransactionManager;
  private final DataSource dataSource;
  private final JobBuilderFactory jobBuilderFactory;
  private final Step artistStep;
  private final Step labelStep;
  private final Step masterStep;
  private final Step releaseStep;
  private final TaskExecutor batchTaskExecutor;

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
    jobLauncher.setTaskExecutor(batchTaskExecutor);
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

  @Bean
  public Job discogsBatchJob() {
    return jobBuilderFactory
        .get(JOB_NAME)
        .start(artistStep).on(FAILED).end()
        .from(artistStep).on(ANY).to(labelStep)
        .from(labelStep).on(FAILED).end()
        .from(labelStep).on(ANY).to(masterStep)
        .from(masterStep).on(FAILED).end()
        .from(masterStep).on(ANY).to(releaseStep)
        .from(releaseStep).on(ANY).end()
        .build()
        .build();
  }

  @Bean
  public Map<DumpType, DiscogsDump> dumpMap() {
    return new HashMap<>();
  }
}