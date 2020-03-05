package io.dsub.dumpdbmgmt.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * Java-based configuration for batch process report.
 * Note that TaskExecutor is shared from here and the ones for job itself.
 */

@Configuration
@PropertySource(value = "classpath:application.properties")
public class AppConfig {

    Environment env;

    public AppConfig(Environment env) {
        this.env = env;
    }

    @Primary
    @Bean(value = "jpaTransactionManager")
    public JpaTransactionManager transactionManager(
            @Qualifier(value = "entityManagerFactory") LocalContainerEntityManagerFactoryBean emf,
            @Qualifier(value = "batchDataSource") DataSource dataSource) {
        JpaTransactionManager manager = new JpaTransactionManager();
        emf.setValidationMode(ValidationMode.CALLBACK);
        manager.setPersistenceUnitName("jpaTransactionPU");
        manager.setDataSource(dataSource);
        manager.setTransactionSynchronization(0);
        manager.setEntityManagerFactory(emf.getObject());
        manager.setValidateExistingTransaction(true);
        manager.setNestedTransactionAllowed(true);
        return manager;
    }

    @Bean("batchTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("Batch_TaskExecutor_");
        taskExecutor.setMaxPoolSize(env.getProperty("taskex.maxpoolsize", Integer.class, 4));
        taskExecutor.setCorePoolSize(env.getProperty("taskex.corepoolsize", Integer.class, 2));
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }
}
