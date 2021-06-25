package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.TestDumpGenerator;
import io.dsub.discogs.batch.config.JpaConfig;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.repository.DiscogsDumpRepository;
import io.dsub.discogs.batch.dump.service.DefaultDiscogsDumpService;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import org.mockito.Mockito;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.spy;

@Configuration
@Import(JpaConfig.class)
@EnableAutoConfiguration
@PropertySource("classpath:application-test.yml")
public class DiscogsJobIntegrationTestConfig {

    @Bean
    public JobLauncherTestUtils getJobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }

    @Bean
    public JobRepositoryTestUtils getJobRepositoryTestUtils() {
        return new JobRepositoryTestUtils();
    }

    @Bean
    public ThreadPoolTaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    public CountDownLatch countDownLatch() {
        return Mockito.spy(new CountDownLatch(1));
    }

    @Bean
    public FileUtil fileUtil() {
        return SimpleFileUtil.builder()
                .appDirectory("discogs-data-batch-test")
                .isTemporary(false)
                .build();
    }

    @Bean
    public DiscogsDumpItemReaderBuilder readerBuilder() {
        return new DiscogsDumpItemReaderBuilder(fileUtil());
    }

    @Bean
    public Map<EntityType, File> dumpFiles() throws IOException, FileException {
        return testDumpGenerator().createDiscogsDumpFiles();
    }

    @Bean
    public ApplicationArguments applicationArguments() {
        String url = "url=jdbc:h2:mem:testdb;MODE=MYSQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DATABASE_TO_UPPER=false";
        String user = "username=sa";
        String pass = "password=";
        return new DefaultApplicationArguments(url, user, pass);
    }

    @Bean
    public TestDumpGenerator testDumpGenerator() throws FileException {
        return new TestDumpGenerator(fileUtil().getAppDirectory(true));
    }

    @Bean
    public DiscogsDumpService dumpService(DiscogsDumpRepository repository) throws IOException, FileException {
        Map<EntityType, File> dumpFiles = dumpFiles();
        DiscogsDumpService dumpService = new DefaultDiscogsDumpService(repository, Mockito.mock(DumpSupplier.class)) {
            @Override
            public void afterPropertiesSet() {
            }

            @Override
            public DiscogsDump getDiscogsDump(String eTag) {
                // i.e call by artist, release, ...
                EntityType type = EntityType.valueOf(eTag.toUpperCase(Locale.ROOT));
                File file = dumpFiles.get(type);

                DiscogsDump dump = new DiscogsDump();
                dump.setUriString(file.getAbsolutePath());
                try {
                    dump.setUrl(file.toURI().toURL());
                } catch (IOException e) {
                    return null;
                }
                dump.setSize(file.length());
                dump.setType(type);
                return dump;
            }
        };
        return spy(dumpService);
    }
}
