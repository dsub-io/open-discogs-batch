package io.dsub.dumpdbmgmt.config;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Typical batch configurer, but with shared TaskExecutor injection.
 *
 * todo: consider more specified job launcher.
 */

@Configuration
@EnableBatchProcessing
public class BatchConfig implements BatchConfigurer {

    DataSource dataSource;
    PlatformTransactionManager tm;
    TaskExecutor taskExecutor;
    JobExplorer jobExplorer;

    public BatchConfig(@Qualifier(value = "batchDataSource") DataSource dataSource,
                       @Qualifier(value = "jpaTransactionManager") PlatformTransactionManager tm,
                       @Qualifier(value = "batchTaskExecutor") TaskExecutor taskExecutor,
                       JobExplorer jobExplorer) {
        this.dataSource = dataSource;
        this.tm = tm;
        this.taskExecutor = taskExecutor;
        this.jobExplorer = jobExplorer;
    }

    @Override
    public JobRepository getJobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(getTransactionManager());
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return this.tm;
    }

    @Override
    public JobLauncher getJobLauncher() throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(getJobRepository());
        jobLauncher.setTaskExecutor(taskExecutor);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Override
    public JobExplorer getJobExplorer() {
        return this.jobExplorer;
    }
}
