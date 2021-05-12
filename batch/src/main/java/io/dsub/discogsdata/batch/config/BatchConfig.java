package io.dsub.discogsdata.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
    public static final int DEFAULT_CHUNK_SIZE = 1000;
    public static final int DEFAULT_THROTTLE_LIMIT = 10;
}