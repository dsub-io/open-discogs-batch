package io.dsub.discogsdata.batch.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.dsub.discogsdata.batch.config.BatchConfig;
import io.dsub.discogsdata.batch.domain.artist.ArtistXML;
import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.batch.job.step.ArtistStepConfig;
import io.dsub.discogsdata.batch.job.step.LabelStepConfig;
import io.dsub.discogsdata.batch.job.step.MasterStepConfig;
import io.dsub.discogsdata.batch.job.step.ReleaseItemStepConfig;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import io.dsub.discogsdata.batch.util.FileUtil;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseRepository;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.reflections.Reflections;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.support.Repositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@SpringBatchTest
@ContextConfiguration(
    classes = {
        BatchConfig.class,
        DiscogsJobIntegrationTestConfig.class,
        ArtistStepConfig.class,
        LabelStepConfig.class,
        MasterStepConfig.class,
        ReleaseItemStepConfig.class
    })
public class DiscogsJobIntegrationTest {
  @Autowired
  ItemStreamReader<ArtistXML> artistStreamReader;
  @RegisterExtension
  LogSpy logSpy = new LogSpy();
  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired
  private JobRepositoryTestUtils jobRepositoryTestUtils;
  @Autowired
  private ArtistRepository artistRepository;
  @Autowired
  private LabelRepository labelRepository;
  @Autowired
  private MasterRepository masterRepository;
  @Autowired
  private ReleaseRepository releaseRepository;
  @Autowired
  private FileUtil fileUtil;
  @Autowired
  @Qualifier("dataSource")
  private DataSource dataSource;
  @Autowired
  private PlatformTransactionManager transactionManager;
  @Autowired
  private ApplicationContext context;
  @Autowired
  private Map<DumpType, DiscogsDump> dumpMap;

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
  @Timeout(10000)
  public void whenJobExecuted__ShouldOnlyContainOneStepExecution() throws Exception {
    JobParameters uniqueJobParams =
        jobLauncherTestUtils
            .getUniqueJobParametersBuilder()
            .addJobParameters(defaultJobParameters())
            .toJobParameters();

    JobExecution jobExecution = jobLauncherTestUtils.launchJob(uniqueJobParams);
    ExitStatus exitStatus = jobExecution.getExitStatus();

    Reflections reflections = new Reflections("io.dsub.discogsdata.common");
    List<Class<? extends BaseEntity>> entityClasses = reflections.getSubTypesOf(BaseEntity.class)
        .stream()
        .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()) && !Modifier
            .isInterface(clazz.getModifiers()))
        .collect(Collectors.toList());

    Repositories repositories = new Repositories(context);

    assertThat(exitStatus.getExitCode(), is("COMPLETED"));

    assertThat(dumpMap.size(), is(4));

    for (DiscogsDump dump : dumpMap.values()) {
      Path filePath = fileUtil.getFilePath(dump.getFileName(), false);
      assertThat(Files.exists(filePath), is(true));
    }

    assertThat(releaseRepository.count(), is(3L));
    assertThat(masterRepository.count(), is(3L));
    assertThat(labelRepository.count(), is(2L));
    assertThat(artistRepository.count(), is(3L));

    // check every single entities have at least one entry.
    entityClasses.forEach(entityClass -> repositories.getRepositoryFor(entityClass)
        .map(repo -> (JpaRepository<?, ?>) repo)
        .ifPresent(
            repo -> assertThat((repo).count(), is(greaterThan(0L)))));
  }
}
