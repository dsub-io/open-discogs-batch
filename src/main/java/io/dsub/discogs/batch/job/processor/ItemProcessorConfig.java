package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.artist.ArtistCommand;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand;
import io.dsub.discogs.batch.domain.label.LabelCommand;
import io.dsub.discogs.batch.domain.label.LabelSubItemsCommand;
import io.dsub.discogs.batch.domain.master.MasterCommand;
import io.dsub.discogs.batch.domain.master.MasterSubItemsCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsCommand;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.common.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Configuration
@RequiredArgsConstructor
public class ItemProcessorConfig {

    private final EntityIdRegistry entityIdRegistry;

    @Bean
    @StepScope
    public ItemProcessor<ArtistCommand, BaseEntity> artistCoreProcessor() {
        return new ArtistCoreProcessor();
    }

    @Bean
    @StepScope
    public ItemProcessor<ArtistSubItemsCommand, Collection<BaseEntity>> artistSubItemsProcessor() {
        return new ArtistSubItemsProcessor(entityIdRegistry);
    }

    @Bean
    @StepScope
    public ItemProcessor<LabelCommand, BaseEntity> labelCoreProcessor() {
        return new LabelCoreProcessor();
    }

    @Bean
    @StepScope
    public ItemProcessor<LabelSubItemsCommand, Collection<BaseEntity>> labelSubItemsProcessor() {
        return new LabelSubItemsProcessor(entityIdRegistry);
    }

    @Bean
    @StepScope
    public ItemProcessor<MasterCommand, BaseEntity> masterCoreProcessor() {
        return new MasterCoreProcessor();
    }

    @Bean
    @StepScope
    public ItemProcessor<MasterSubItemsCommand, Collection<BaseEntity>> masterSubItemsProcessor() {
        return new MasterSubItemsProcessor(entityIdRegistry);
    }

    @Bean
    @StepScope
    public ItemProcessor<ReleaseItemCommand, BaseEntity> releaseItemCoreProcessor() {
        return new ReleaseItemCoreProcessor();
    }

    @Bean
    @StepScope
    public ItemProcessor<ReleaseItemSubItemsCommand, Collection<BaseEntity>> releaseItemSubItemsProcessor() {
        return new ReleaseItemSubItemsProcessor(entityIdRegistry);
    }
}
