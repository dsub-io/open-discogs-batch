package io.dsub.discogs.batch.job.writer;

import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import io.dsub.discogs.batch.job.ImportJobParameters;
import io.dsub.discogs.batch.job.progress.ImportProgressStore;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.UpdatableRecord;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemWriterConfig {

  public static final String ENTITY_ITEM_WRITER = "entityItemWriter";

  private final DSLContext context;
  private final DataSource dataSource;

  @Bean
  public ItemWriter<UpdatableRecord<?>> jooqItemWriter() {
      return new DefaultLJooqItemWriter<>(context);
  }

  @Bean
  @StepScope
  @Primary
  public ItemWriter<UpdatableRecord<?>> entityItemWriter(
      ImportProgressStore progressStore,
      @Value("#{jobParameters['" + ImportJobParameters.RUN_ID + "']}") Long runId) {
    return new ActiveRunItemWriter<>(jooqItemWriter(), progressStore, runId);
  }

  @Bean
  @StepScope
  public ItemWriter<MasterRecord> postgresJooqMasterMainReleaseItemWriter(
      ImportProgressStore progressStore,
      @Value("#{jobParameters['" + ImportJobParameters.RUN_ID + "']}") Long runId) {
    return new ActiveRunItemWriter<>(
        new DefaultJooqMasterMainReleaseItemWriter(context), progressStore, runId);
  }

  @Bean
  public DurableRelationItemWriterFactory durableRelationItemWriterFactory(
      ImportProgressStore progressStore) {
    return new DurableRelationItemWriterFactory(dataSource, jooqItemWriter(), progressStore);
  }
}
