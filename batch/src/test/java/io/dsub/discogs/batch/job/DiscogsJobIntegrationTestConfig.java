package io.dsub.discogs.batch.job;

import static org.junit.jupiter.api.Assertions.fail;

import com.zaxxer.hikari.HikariDataSource;
import io.dsub.discogs.batch.TestDumpGenerator;
import io.dsub.discogs.batch.datasource.DBType;
import io.dsub.discogs.batch.datasource.DataSourceProperties;
import io.dsub.discogs.batch.datasource.SimpleDataSourceProperties;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.job.reader.DiscogsDumpItemReaderBuilder;
import io.dsub.discogs.batch.query.JpaEntityQueryBuilder;
import io.dsub.discogs.batch.query.MySQLJpaEntityQueryBuilder;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.SimpleFileUtil;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.exception.DumpNotFoundException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.persistence.ValidationMode;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
@EnableTransactionManagement
@PropertySource("classpath:application-batch-test.yml")
@EnableJpaRepositories(basePackages = {"io.dsub.discogs.common", "io.dsub.discogs.batch.dump"})
@EntityScan(basePackages = {"io.dsub.discogs.common", "io.dsub.discogs.batch"})
public class DiscogsJobIntegrationTestConfig {

  @Value("classpath:mysql-test-schema.sql")
  private Resource mysqlSchema;

  @Bean
  public JpaEntityQueryBuilder<BaseEntity> jpaEntityQueryBuilder() {
    return new MySQLJpaEntityQueryBuilder();
  }

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
  public HikariDataSource dataSource() {
    HikariDataSource dataSource = new HikariDataSource();
    Properties properties = new Properties();
    properties.setProperty("rewriteBatchedStatements", "true");
    dataSource.setDataSourceProperties(properties);
    dataSource.setJdbcUrl("jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    dataSource.setDriverClassName("org.h2.Driver");
    initializeDb(dataSource);
    return dataSource;
  }

  @Bean
  public DataSourceProperties dataSourceProperties() {
    return SimpleDataSourceProperties.builder().dbType(DBType.MYSQL).build();
  }

  @Bean
  public JdbcTemplate jdbcTemplate() {
    return new JdbcTemplate(dataSource());
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

  private void initializeDb(DataSource dataSource) {
    try (Connection conn = dataSource.getConnection()) {
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(mysqlSchema.getInputStream()));
      String sql = reader.lines().collect(Collectors.joining("\n"));
      List<String> queries =
          Arrays.stream(sql.split(";")).map(String::trim).collect(Collectors.toList());
      for (String query : queries) {
        query = conn.nativeSQL(query);
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
          stmt.closeOnCompletion();
          stmt.execute();
          try {
            conn.commit();
          } catch (SQLException e) {
            conn.rollback();
            throw e;
          }
        }
      }
    } catch (SQLException | IOException e) {
      fail("failed to initialize test schema... " + e.getMessage());
    }
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean entityManagerFactoryBean =
        new LocalContainerEntityManagerFactoryBean();
    entityManagerFactoryBean.setDataSource(dataSource());
    entityManagerFactoryBean.setJpaDialect(new HibernateJpaDialect());
    entityManagerFactoryBean.setPackagesToScan("io.dsub.discogs.batch", "io.dsub.discogs.common");
    entityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    Properties properties = new Properties();
    properties.setProperty("hibernate.format_sql", "true");
    properties.setProperty("hibernate.order_inserts", "true");
    properties.setProperty("hibernate.order_updates", "true");
    properties.setProperty("hibernate.jdbc.batch_size", "20000");
    properties.setProperty("hibernate.jdbc.time_zone", "UTC");
    properties.setProperty("database-platform", "org.hibernate.dialect.MySQLDialect");
    entityManagerFactoryBean.setJpaProperties(properties);
    entityManagerFactoryBean.setValidationMode(ValidationMode.AUTO);
    entityManagerFactoryBean.afterPropertiesSet();
    return entityManagerFactoryBean;
  }

  @Bean
  public PlatformTransactionManager transactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setDataSource(dataSource());
    transactionManager.setValidateExistingTransaction(true);
    transactionManager.setRollbackOnCommitFailure(true);
    transactionManager.setNestedTransactionAllowed(true);
    transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
    transactionManager.setTransactionSynchronization(1);
    return transactionManager;
  }

  @Bean
  public Map<DumpType, File> dumpFiles() throws IOException {
    return new TestDumpGenerator(fileUtil().getAppDirectory(true)).createDiscogsDumpFiles();
  }

  @Bean
  public DiscogsDumpService dumpService() throws IOException {

    Map<DumpType, File> dumpFiles = dumpFiles();

    return new DiscogsDumpService() {
      @Override
      public void updateDB() {}

      @Override
      public boolean exists(String eTag) {
        return false;
      }

      @Override
      public DiscogsDump getDiscogsDump(String eTag) {
        // i.e call by artist, release, ...
        DumpType type = DumpType.valueOf(eTag.toUpperCase(Locale.ROOT));
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

      @Override
      public DiscogsDump getMostRecentDiscogsDumpByType(DumpType type) {
        return null;
      }

      @Override
      public DiscogsDump getMostRecentDiscogsDumpByTypeYearMonth(
          DumpType type, int year, int month) {
        return null;
      }

      @Override
      public Collection<DiscogsDump> getAllByTypeYearMonth(
          List<DumpType> types, int year, int month) {
        return null;
      }

      @Override
      public List<DiscogsDump> getDumpByTypeInRange(DumpType type, int year, int month) {
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
}
