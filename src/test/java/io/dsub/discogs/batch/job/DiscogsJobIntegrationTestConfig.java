package io.dsub.discogs.batch.job;

import static org.mockito.Mockito.spy;

import io.dsub.discogs.batch.TestDumpGenerator;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DiscogsDumpVerifier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@PropertySource("classpath:application-test.yml")
public class DiscogsJobIntegrationTestConfig {

  @Bean
  public JobOperatorTestUtils getJobOperatorTestUtils(
      JobOperator jobOperator, JobRepository jobRepository) {
    return new JobOperatorTestUtils(jobOperator, jobRepository);
  }

  @Bean
  public JobRepositoryTestUtils getJobRepositoryTestUtils(JobRepository jobRepository) {
    return new JobRepositoryTestUtils(jobRepository);
  }

  @Bean
  public PlatformTransactionManager transactionManager(javax.sql.DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
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
  public DiscogsDumpVerifier dumpVerifier() {
    return new DiscogsDumpVerifier();
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
        return new DiscogsDump(eTag, type, file.getAbsolutePath(), file.length(), LocalDate.now(),
            null);
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

      @Override
      public List<DiscogsDump> resolveLatest(Set<EntityType> types) {
        return null;
      }

      @Override
      public List<DiscogsDump> resolveMonth(Set<EntityType> types, YearMonth month) {
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
}
