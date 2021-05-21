package io.dsub.discogsdata.batch.job.writer;

import io.dsub.discogsdata.common.entity.artist.Artist;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import java.util.Collection;
import javax.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ItemWriterConfig {

  private final EntityManagerFactory emf;

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
    writer.setClassifier(classifiable -> getItemWriter());
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
}
