package io.dsub.discogs.batch.job;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.TestDumpGenerator;
import io.dsub.discogs.batch.config.BatchConfig;
import io.dsub.discogs.batch.container.PostgreSQLContainerBaseTest;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBatchTest
@ContextConfiguration(classes = {
        BatchConfig.class,
        BatchInfrastructureConfig.class,
        DiscogsJobIntegrationTestConfig.class}
)
public class DiscogsJobIntegrationTest extends PostgreSQLContainerBaseTest {

    @Autowired
    JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    FileUtil fileUtil;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Map<EntityType, DiscogsDump> dumpMap;

    @Autowired
    Map<EntityType, File> dumpFiles;

    @Autowired
    CountDownLatch exitLatch;

    @RegisterExtension
    LogSpy logSpy = new LogSpy();

    @BeforeEach
    void setUp() throws Exception {
        JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
        jobRepositoryFactoryBean.setDataSource(dataSource);
        jobRepositoryFactoryBean.setTransactionManager(transactionManager);
        jobRepositoryFactoryBean.afterPropertiesSet();
        JobRepository jobRepository = jobRepositoryFactoryBean.getObject();
        assertNotNull(jobRepository);
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
    }

    @AfterEach
    public void cleanUp() throws FileException, IOException {
        jobRepositoryTestUtils.removeJobExecutions();
        dumpMap.clear();
        dumpFiles = new TestDumpGenerator(fileUtil.getAppDirectory(true)).createDiscogsDumpFiles();
        exitLatch = spy(new CountDownLatch(1));
    }

    @AfterAll
    static void afterAll() throws FileException {
        FileUtil fileUtil =
                SimpleFileUtil.builder().appDirectory("discogs-data-batch-test").isTemporary(false).build();
        fileUtil.clearAll();
    }

    @Bean
    public ApplicationArguments applicationArguments() {
        return new DefaultApplicationArguments(jdbcUrl, username, password);
    }

    private JobParameters defaultJobParameters() {
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addString("artist", "artist");
        paramsBuilder.addString("label", "label");
        paramsBuilder.addString("master", "master");
        paramsBuilder.addString("release", "release");
        paramsBuilder.addString("chunkSize", "1000");
        return paramsBuilder.toJobParameters();
    }

    @Test
    void whenAllTypesProvided__ShouldNotSkipAnyType() throws Exception {
        JobParameters params =
                jobLauncherTestUtils
                        .getUniqueJobParametersBuilder()
                        .addJobParameters(defaultJobParameters())
                        .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
        ExitStatus exitStatus = jobExecution.getExitStatus();

        verify(exitLatch, times(1)).countDown();

        assertThat(exitStatus.getExitCode(), is("COMPLETED"));
        assertThat(dumpMap.size(), is(4));

        for (DiscogsDump dump : dumpMap.values()) {
            Path filePath = fileUtil.getFilePath(dump.getFileName(), false);
            assertThat(Files.exists(filePath), is(true));
        }

        //     check every single entities have at least one entry.
//        getRepositories(entityClasses, repositories)
//                .forEach(repo -> assertThat(repo.count(), greaterThan(0L)));
    }

    @Test
    void whenOnlyArtistLabel__ShouldSkipMasterRelease() throws Exception {
        JobParametersBuilder builder = new JobParametersBuilder();
        //    builder.addString("artist", "artist");
        builder.addString("label", "label");
        builder.addString("chunkSize", "1000");
        JobParameters params =
                jobLauncherTestUtils
                        .getUniqueJobParametersBuilder()
                        .addJobParameters(builder.toJobParameters())
                        .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(params);
        ExitStatus exitStatus = jobExecution.getExitStatus();

        List<String> logs = logSpy.getLogsByExactLevelAsString(Level.INFO, true, "io.dsub.discogs");

        assertThat(exitStatus.getExitCode(), is("COMPLETED"));
        assertThat(dumpMap.size(), is(1));
        assertThat(logs, hasItems(
                "artist eTag not found. skipping artist step.",
                "label eTag found. executing label step.",
                "master eTag not found. skipping master step.",
                "release eTag not found. skipping release step."
        ));
    }
}
