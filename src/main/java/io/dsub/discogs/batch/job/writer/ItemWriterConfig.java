package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.common.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBuilder;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Collection;
import java.util.Properties;

import static io.dsub.discogs.batch.config.JpaConfig.COMMON;
import static io.dsub.discogs.batch.config.JpaConfig.DUMP;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemWriterConfig {

    private final EntityManagerFactory entityManagerFactory;

//    @Bean
//    @StepScope
//    public JpaItemWriter<BaseEntity> entityItemWriter() {
//        return new JpaItemWriterBuilder<BaseEntity>()
//                .entityManagerFactory(emf)
//                .build();
//    }

    @Bean
    @StepScope
    public CustomJpaItemWriter<BaseEntity> entityItemWriter() throws Exception {
        CustomJpaItemWriter<BaseEntity> writer = new CustomJpaItemWriter<>(entityManagerFactory);
        writer.afterPropertiesSet();
        return writer;
    }

    @Bean
    @StepScope
    public ItemWriter<Collection<BaseEntity>> baseEntityCollectionItemWriter() throws Exception {
        return getBaseEntityCollectionItemWriter(entityItemWriter());
    }

    private BaseEntityCollectionItemWriter getBaseEntityCollectionItemWriter(ItemWriter<BaseEntity> itemWriter) {
        return new BaseEntityCollectionItemWriter(itemWriter);
    }
}