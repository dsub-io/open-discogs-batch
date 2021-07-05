package io.dsub.discogs.batch.datasource;

import io.dsub.discogs.batch.util.DataSourceUtil;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class BatchDataSourceScriptResourceConverter implements Function<DataSource, Resource> {

  private static final String BASE_PATH = "schema/";
  private static final String POSTGRES_INIT_PATH = BASE_PATH + "postgresql-batch-init.sql";

  @Override
  public Resource apply(DataSource dataSource) {
    DBType type = DataSourceUtil.getDBTypeFrom(dataSource);
    if (type == null) {
      return null;
    }

    ResourceLoader resourceLoader = new DefaultResourceLoader();

    return switch (type) {
      case POSTGRESQL -> resourceLoader.getResource(POSTGRES_INIT_PATH);
    };
  }
}