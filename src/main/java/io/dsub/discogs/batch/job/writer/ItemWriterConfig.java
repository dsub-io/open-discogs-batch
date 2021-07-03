package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.common.jooq.postgres.tables.records.MasterRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.UpdatableRecord;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemWriterConfig {

    private final DSLContext context;

    @Bean
    public ItemWriter<UpdatableRecord<?>> jooqItemWriter() {
        return new PostgresJooqItemWriter<>(context);
    }

    @Bean
    public ItemWriter<Collection<UpdatableRecord<?>>> baseEntityCollectionItemWriter() {
        return getBaseEntityCollectionItemWriter(jooqItemWriter());
    }

    @Bean
    @StepScope
    public ItemWriter<MasterRecord> postgresJooqMasterMainReleaseItemWriter() {
        return new PostgresJooqMasterMainReleaseItemWriter(context);
    }

    private CollectionItemWriter<UpdatableRecord<?>> getBaseEntityCollectionItemWriter(ItemWriter<UpdatableRecord<?>> delegate) {
        return new CollectionItemWriter<>(delegate);
    }
}