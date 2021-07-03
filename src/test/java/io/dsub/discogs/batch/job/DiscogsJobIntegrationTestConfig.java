package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.TestDumpGenerator;
import io.dsub.discogs.batch.container.PostgreSQLContainerBaseTest;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
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
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:application-test.yml")
public class DiscogsJobIntegrationTestConfig extends PostgreSQLContainerBaseTest {

    @Bean
    public JobLauncherTestUtils getJobLauncherTestUtils() {
        return new JobLauncherTestUtils();
    }

    @Bean
    public JobRepositoryTestUtils getJobRepositoryTestUtils() {
        return new JobRepositoryTestUtils();
    }

    @Bean
    public DataSource dataSource() {
        return DiscogsJobIntegrationTest.dataSource;
    }

    @Bean
    public ApplicationArguments applicationArguments() {
        return new DefaultApplicationArguments(CONTAINER.getJdbcUrl(), CONTAINER.getUsername(), CONTAINER.getPassword());
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
    public FileUtil fileUtil() {
        return SimpleFileUtil.builder()
                .appDirectory("discogs-data-batch-test")
                .isTemporary(false)
                .build();
    }

    @Bean
    CountDownLatch countDownLatch() {
        return spy(new CountDownLatch(1));
    }

    @Bean
    public DiscogsDumpService dumpService() throws IOException, FileException {

        Map<EntityType, File> dumpFiles = dumpFiles();

        return new DiscogsDumpService() {
            @Override
            public void updateDB() {
            }

            @Override
            public boolean exists(String eTag) {
                return false;
            }

            @Override
            public DiscogsDump getDiscogsDump(String eTag) {
                // i.e call by artist, release, ...
                EntityType type = EntityType.valueOf(eTag.toUpperCase(Locale.ROOT));
                File file = dumpFiles.get(type);
                return new DiscogsDump(eTag, type, file.getAbsolutePath(), file.length(), LocalDate.now(), null);
            }
            @Override
            public DiscogsDump getMostRecentDiscogsDumpByType(EntityType type) {
                return null;
            }

            @Override
            public DiscogsDump getMostRecentDiscogsDumpByTypeYearMonth(
                    EntityType type, int year, int month) {
                return null;
            }

            @Override
            public Collection<DiscogsDump> getAllByTypeYearMonth(
                    List<EntityType> types, int year, int month) {
                return null;
            }

            @Override
            public List<DiscogsDump> getDumpByTypeInRange(EntityType type, int year, int month) {
                return null;
            }

            @Override
            public List<DiscogsDump> getLatestCompleteDumpSet() throws DumpNotFoundException {
                return null;
            }

            @Override
            public List<DiscogsDump> getAll() {
                return null;
            }
        };
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
    public TestDumpGenerator testDumpGenerator() throws FileException {
        return new TestDumpGenerator(fileUtil().getAppDirectory(true));
    }

//    @Bean
//    public DiscogsDumpRepository repository() {
//        return Mockito.mock(DiscogsDumpRepository.class);
//    }
}
