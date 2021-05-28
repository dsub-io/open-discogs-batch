package io.dsub.discogsdata.batch.job.step;

import javax.sql.DataSource;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;

public abstract class AbstractStepConfig {

  protected static final String CHUNK = "#{jobParameters['chunkSize']}";
  protected static final String THROTTLE = "#{jobParameters['throttleLimit']}";
  protected static final String ANY = "*";
  protected static final String FAILED = "FAILED";

  protected <T> ItemWriter<T> buildItemWriter(String query, DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<T>()
        .sql(query)
        .dataSource(dataSource)
        .beanMapped()
        .assertUpdates(false)
        .build();
  }
}
