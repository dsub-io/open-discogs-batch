package io.dsub.discogs.batch.config;

import io.dsub.discogs.batch.datasource.DataSourceDetails;
import io.dsub.discogs.batch.util.DataSourceUtil;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure support for JOOQ configuration, providing the {@link DSLContext} bean.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class JooqConfig {

  private final DataSource dataSource;
  private final DatabaseSchema databaseSchema;

  @Bean
  public DSLContext dslContext() {
    DataSourceDetails details = dataSourceDetails();
    Settings settings =
        new Settings()
            .withRenderMapping(
                new RenderMapping()
                    .withSchemata(
                        new MappedSchema()
                            .withInput(DatabaseSchema.DEFAULT_NAME)
                            .withOutput(databaseSchema.name())));
    return DSL.using(dataSource, details.dialect(), settings);
  }

  @Bean
  public DataSourceDetails dataSourceDetails() {
    return DataSourceUtil.getDataSourceDetails(dataSource);
  }
}
