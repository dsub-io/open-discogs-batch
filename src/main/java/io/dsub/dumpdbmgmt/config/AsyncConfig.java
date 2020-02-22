package io.dsub.dumpdbmgmt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * TaskExecutor config for entire job process.
 * Be aware that at least one thread will be bound to the job-repository.
 * Also, as of we are reading a single xml file, the reading speed may vary and
 * it is highly doubted to use more than 3 threads for processors and writers
 * will bring better performance. (But you may try)
 *
 * NOTE: @EnableAsync is currently redundant, but remained for future reference.
 */

@EnableAsync
@Configuration
@PropertySource(value = "classpath:application.properties")
public class AsyncConfig {

    Environment env;

    public AsyncConfig(Environment env) {
        this.env = env;
    }

    @Bean("batchTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("Batch_TaskExecutor_");
        taskExecutor.setMaxPoolSize(env.getProperty("taskex.maxpoolsize", Integer.class, 4));
        taskExecutor.setCorePoolSize(env.getProperty("taskex.corepoolsize", Integer.class, 2));
        taskExecutor.afterPropertiesSet();
        return taskExecutor;
    }
}
