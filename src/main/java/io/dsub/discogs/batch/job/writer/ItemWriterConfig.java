package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.common.jooq.tables.records.MasterRecord;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.UpdatableRecord;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
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
  public ItemWriter<Collection<UpdatableRecord<?>>> baseEntityCollectionItemWriter() {
    return getBaseEntityCollectionItemWriter(jooqItemWriter());
  }

  @Bean
  @StepScope
  public ItemWriter<MasterRecord> postgresJooqMasterMainReleaseItemWriter() {
    return new DefaultJooqMasterMainReleaseItemWriter(context);
  }

  private CollectionItemWriter<UpdatableRecord<?>> getBaseEntityCollectionItemWriter(
      ItemWriter<UpdatableRecord<?>> delegate) {
    return new CollectionItemWriter<>(delegate);
  }
}
