package io.dsub.discogs.batch.job.writer;

import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.UpdatableRecord;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemWriterConfig {

  private final DSLContext context;

  @Bean
  public ItemWriter<UpdatableRecord<?>> jooqItemWriter() {
      return new DefaultLJooqItemWriter<>(context);
  }

  @Bean
  @StepScope
  public ItemWriter<Collection<UpdatableRecord<?>>> baseEntityCollectionItemWriter(
      @Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
    return getBaseEntityCollectionItemWriter(jooqItemWriter(), chunkSize);
  }

  @Bean
  @StepScope
  public ItemWriter<MasterRecord> postgresJooqMasterMainReleaseItemWriter() {
    return new DefaultJooqMasterMainReleaseItemWriter(context);
  }

  private CollectionItemWriter<UpdatableRecord<?>> getBaseEntityCollectionItemWriter(
      ItemWriter<UpdatableRecord<?>> delegate, int maxBatchSize) {
    return new CollectionItemWriter<>(delegate, maxBatchSize);
  }
}
