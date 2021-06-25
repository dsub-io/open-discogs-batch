package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.job.listener.BatchListenerConfig;
import io.dsub.discogs.batch.job.processor.ItemProcessorConfig;
import io.dsub.discogs.batch.job.reader.ItemReaderConfig;
import io.dsub.discogs.batch.job.step.GlobalStepConfig;
import io.dsub.discogs.batch.job.writer.ItemWriterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Import(value = {
        GlobalStepConfig.class,
        ItemReaderConfig.class,
        ItemProcessorConfig.class,
        ItemWriterConfig.class,
        BatchListenerConfig.class})
public class BatchInfrastructureConfig {
    @Bean
    public Map<EntityType, DiscogsDump> dumpMap() {
        return new HashMap<>();
    }
}
