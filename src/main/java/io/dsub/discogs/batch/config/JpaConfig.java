package io.dsub.discogs.batch.config;

import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = {JpaConfig.COMMON, JpaConfig.DUMP})
@EnableJpaRepositories(basePackages = {JpaConfig.COMMON, JpaConfig.DUMP}, transactionManagerRef = "stepTransactionManager")
@RequiredArgsConstructor
public class JpaConfig {
    public static final String BASE_PKG = "io.dsub.discogs";
    public static final String COMMON = BASE_PKG + ".common";
    public static final String DUMP = BASE_PKG + ".batch.dump";

    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    @Bean(name = "stepTransactionManager")
    public PlatformTransactionManager stepTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager(entityManagerFactory);
        transactionManager.setDataSource(dataSource);
        transactionManager.setJpaProperties(getJpaProperties());
        transactionManager.afterPropertiesSet();
        return transactionManager;
    }

    private Properties getJpaProperties() {
        Properties props = new Properties();
        props.put("hibernate.jdbc.batch_size", "500");
        props.put("hibernate.jdbc.time_zone", "UTC");
        props.put("hibernate.jdbc.batch_versioned_data", "true");
        props.put("hibernate.order_inserts", "true");
        props.put("hibernate.order_updates", "true");
        props.put("hibernate.format_sql", "true");
        return props;
    }
}