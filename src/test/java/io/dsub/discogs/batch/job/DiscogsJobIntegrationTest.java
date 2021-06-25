package io.dsub.discogs.batch.job;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.config.BatchConfig;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import io.dsub.discogs.common.artist.entity.Artist;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.genre.entity.Genre;
import io.dsub.discogs.common.label.entity.Label;
import io.dsub.discogs.common.master.entity.Master;
import io.dsub.discogs.common.release.entity.ReleaseItem;
import io.dsub.discogs.common.style.entity.Style;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reflections.Reflections;
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
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
@SpringBatchTest
@ContextConfiguration(classes = {
        BatchConfig.class,
        DiscogsJobIntegrationTestConfig.class,
        BatchInfrastructureConfig.class}
)
public class DiscogsJobIntegrationTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private CountDownLatch exitLatch;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private Map<EntityType, DiscogsDump> dumpMap;
    @Autowired
    private DataSource dataSource;

    @RegisterExtension
    LogSpy logSpy = new LogSpy();
    Reflections reflections = new Reflections("io.dsub.discogs.common");
    Repositories repositories;
    List<Class<? extends BaseEntity>> entityClasses =
            reflections.getSubTypesOf(BaseEntity.class).stream()
                    .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers())
                            && !Modifier.isInterface(clazz.getModifiers()))
                    .collect(Collectors.toList());

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
    public void cleanUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        dumpMap.clear();
//        clearTables(repositories);
    }

    @AfterAll
    static void afterAll() throws FileException {
        FileUtil fileUtil =
                SimpleFileUtil.builder().appDirectory("discogs-data-batch-test").isTemporary(false).build();
        fileUtil.clearAll();
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
    void save(@Autowired final DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Map<String, Object>> queryResult = jdbcTemplate.queryForList("SELECT table_name FROM information_schema.tables");
        for (Map<String, Object> map : queryResult) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                System.out.println(entry.getKey() + " >> " + entry.getValue().toString());
            }
        }
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

        repositories = new Repositories(context);

        for (DiscogsDump dump : dumpMap.values()) {
            Path filePath = fileUtil.getFilePath(dump.getFileName(), false);
            assertThat(Files.exists(filePath), is(true));
        }

        assertThat(getRepositoryFor(Artist.class, repositories).count(), is(3L));
        assertThat(getRepositoryFor(Label.class, repositories).count(), is(2L));
        assertThat(getRepositoryFor(Master.class, repositories).count(), is(3L));
        assertThat(getRepositoryFor(ReleaseItem.class, repositories).count(), is(3L));

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

        repositories = new Repositories(context);
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

    private void clearTables(Repositories repositories) {
        List<Class<? extends BaseEntity>> coreEntities =
                List.of(Artist.class, Master.class, Label.class, ReleaseItem.class, Style.class, Genre.class);

        List<Class<? extends BaseEntity>> subEntities = entityClasses.stream()
                .filter(entityClass -> !coreEntities.contains(entityClass))
                .collect(Collectors.toList());

        getRepositories(subEntities, repositories)
                .forEach(CrudRepository::deleteAll);

        getRepositories(coreEntities, repositories)
                .forEach(CrudRepository::deleteAll);
    }

    private List<JpaRepository<?, ?>> getRepositories(List<Class<? extends BaseEntity>> entityClasses, Repositories repositories) {
        return entityClasses.stream()
                .map(repositories::getRepositoryFor)
                .map(optional -> optional.orElse(null))
                .filter(Objects::nonNull)
                .map(repo -> (JpaRepository<?, ?>) repo)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseEntity> JpaRepository<T, ?> getRepositoryFor(Class<T> entityClass, Repositories repositories) {
        Object repoObj = repositories.getRepositoryFor(entityClass).orElse(null);
        if (repoObj == null) {
            throw new InvalidArgumentException("repository for " + entityClass.getSimpleName() + " does not exists");
        }
        return (JpaRepository<T, ?>) repoObj;
    }
}
