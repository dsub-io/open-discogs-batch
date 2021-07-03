package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.common.artist.repository.ArtistRepository;
import io.dsub.discogs.common.label.repository.LabelRepository;
import io.dsub.discogs.common.master.repository.MasterRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
@RequiredArgsConstructor
public class BatchListenerConfig {
    @Bean
    public EntityIdRegistry entityIdRegistry() {
        return new EntityIdRegistry();
    }

    @Bean
    public AtomicLong itemsCounter() {
        return new AtomicLong();
    }

    @Bean
    public IdCachingItemProcessListener idCachingItemProcessListener() {
        return new IdCachingItemProcessListener(entityIdRegistry());
    }

    @Bean
    public ItemCountingItemProcessListener ItemCountingItemProcessListener() {
        return new ItemCountingItemProcessListener(itemsCounter());
    }

    @Bean
    public CacheInversionStepExecutionListener cacheInversionStepExecutionListener() {
        return new CacheInversionStepExecutionListener(entityIdRegistry());
    }

    @Bean
    public StopWatchStepExecutionListener stopWatchStepExecutionListener() {
        return new StopWatchStepExecutionListener(itemsCounter());
    }

    @Bean
    public StringNormalizingItemReadListener stringNormalizingItemReadListener() {
        return new StringNormalizingItemReadListener();
    }

    @Bean
    public ExitSignalJobExecutionListener exitSignalJobExecutionListener(CountDownLatch exitLatch) {
        return new ExitSignalJobExecutionListener(exitLatch);
    }
    @Bean
    public IdCachingJobExecutionListener idCachingJobExecutionListener(DSLContext context) {
        return new IdCachingJobExecutionListener(entityIdRegistry(), context);
    }
    @Bean
    public ClearanceJobExecutionListener clearanceJobExecutionListener(FileUtil fileUtil) {
        return new ClearanceJobExecutionListener(entityIdRegistry(), fileUtil);
    }
}