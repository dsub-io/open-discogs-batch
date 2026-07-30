package io.dsub.discogs.batch.job.writer;

import org.jooq.Query;
import org.jooq.UpdatableRecord;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

public interface JooqItemWriter<T extends UpdatableRecord<?>> extends ItemWriter<T> {

  @Override
  void write(Chunk<? extends T> items);

  Query getQuery(T record);
}
