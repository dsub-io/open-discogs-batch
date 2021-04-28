package io.dsub.discogsdata.batch.config;

import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    public static final String JOB_NAME = "discogs-batch";
    public static final int DEFAULT_CHUNK_SIZE = 3000;
    public static final int DEFAULT_THROTTLE_LIMIT = 10;
    public static int CHUNK_SIZE = DEFAULT_CHUNK_SIZE;
    public static int THROTTLE_LIMIT = DEFAULT_THROTTLE_LIMIT;

    @Bean
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(12);
        executor.setMaxPoolSize(20);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(Boolean.TRUE);
        executor.setThreadNamePrefix(JOB_NAME + "-");
        executor.initialize();
        return executor;
    }

    public static final ProgressBarBuilder PROGRESS_BAR_BUILDER =
            new ProgressBarBuilder()
                    .setStyle(ProgressBarStyle.ASCII)
                    .setUnit("KB", 1024)
                    .setUpdateIntervalMillis(100)
                    .startsFrom(0, Duration.ZERO)
                    .showSpeed()
                    .setConsumer(new ConsoleProgressBarConsumer(System.out, 100));
}
