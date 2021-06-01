package io.dsub.discogsdata.batch.job;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.dsub.discogsdata.batch.config.BatchConfig;
import io.dsub.discogsdata.batch.domain.artist.ArtistXML;
import io.dsub.discogsdata.batch.job.step.ArtistStepConfig;
import io.dsub.discogsdata.batch.job.step.LabelStepConfig;
import io.dsub.discogsdata.batch.job.step.MasterStepConfig;
import io.dsub.discogsdata.batch.job.step.ReleaseItemStepConfig;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import io.dsub.discogsdata.common.repository.artist.ArtistRepository;
import io.dsub.discogsdata.common.repository.label.LabelRepository;
import io.dsub.discogsdata.common.repository.master.MasterRepository;
import io.dsub.discogsdata.common.repository.release.ReleaseRepository;
import java.nio.file.Path;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
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

  @TempDir
  static Path TEMP_DIR;
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
  @Qualifier("dataSource")
  private DataSource dataSource;
  @Autowired
  private PlatformTransactionManager transactionManager;

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

    assertThat(exitStatus.getExitCode(), is("COMPLETED"));
    assertThat(releaseRepository.count(), is(3L));
    assertThat(masterRepository.count(), is(3L));
    assertThat(labelRepository.count(), is(2L));
    assertThat(artistRepository.count(), is(3L));
  }
}
