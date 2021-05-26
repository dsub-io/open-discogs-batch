package io.dsub.discogsdata.batch.job.writer;

import io.dsub.discogsdata.batch.query.JpaEntityQueryBuilder;
import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import java.util.Collection;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ItemWriterConfig {

  private final EntityManagerFactory emf;
  private final JpaEntityQueryBuilder<BaseEntity> queryBuilder;
  private final DataSource dataSource;

  @Bean
  @StepScope
  public ItemWriter<Artist> artistItemWriter() {
    return getItemWriter();
  }

  @Bean
  @StepScope
  public ItemWriter<Collection<BaseEntity>> artistSubItemsWriter() throws Exception {
    ClassifierCompositeCollectionItemWriter<BaseEntity> writer =
        new ClassifierCompositeCollectionItemWriter<>();
    writer.setClassifier(classifiable -> buildItemWriter(classifiable.getClass()));
    writer.afterPropertiesSet();
    return writer;
  }

  @Bean
  @StepScope
  public <T> JpaItemWriter<T> getItemWriter() {
    JpaItemWriter<T> writer = new JpaItemWriter<>();
    writer.setEntityManagerFactory(emf);
    return writer;
  }

  private <T extends BaseEntity> ItemWriter<T> buildItemWriter(
      Class<? extends BaseEntity> entityClass) {
    return new JdbcBatchItemWriterBuilder<T>()
        .sql(queryBuilder.getUpsertQuery(entityClass))
        .dataSource(dataSource)
        .assertUpdates(false)
        .beanMapped()
        .build();
  }
}
