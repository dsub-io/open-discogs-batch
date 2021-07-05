package io.dsub.discogs.batch;

import io.dsub.discogs.batch.datasource.BatchDataSourceScriptResourceConverter;
import io.dsub.discogs.batch.datasource.DBType;
import io.dsub.discogs.batch.datasource.DataSourceDetails;
import io.dsub.discogs.batch.util.DataSourceUtil;
import io.dsub.discogs.common.jooq.Tables;
import java.util.Arrays;
import java.util.Objects;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.TableImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@RequiredArgsConstructor
public class DataSourceInitializerConfig {

  private final DataSource dataSource;

  @Bean
  public DataSourceInitializer dataSourceInitializer() {
    DataSourceInitializer initializer = new DataSourceInitializer();

    // set required beans
    initializer.setDataSource(dataSource);
    initializer.setDatabasePopulator(getDatabasePopulator());
    initializer.afterPropertiesSet();

    return initializer;
  }

  protected DatabasePopulator getDatabasePopulator() {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    BatchDataSourceScriptResourceConverter converter = getBatchDataSourceScriptResourceConverter();
    populator.addScript(converter.apply(dataSource));
    DataSourceDetails details = DataSourceUtil.getDataSourceDetails(dataSource);
    if (!details.type().equals(DBType.POSTGRESQL)) {
      setCharsetAndCollate();
    }
    return populator;
  }

  protected void setCharsetAndCollate() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
    Arrays.stream(Tables.class.getFields())
        .map(field -> {
          try {
            return field.get(Tables.class);
          } catch (IllegalAccessException e) {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .map(tbl -> (TableImpl<?>) tbl)
        .forEach(tbl -> jdbcTemplate.execute("ALTER TABLE " + tbl.getName() + " CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"));
    jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
  }

  protected BatchDataSourceScriptResourceConverter getBatchDataSourceScriptResourceConverter() {
    return new BatchDataSourceScriptResourceConverter();
  }
}
