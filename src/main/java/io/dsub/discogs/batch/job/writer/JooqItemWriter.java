package io.dsub.discogs.batch.job.writer;

import org.jooq.Query;
import org.jooq.UpdatableRecord;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public interface JooqItemWriter<T extends UpdatableRecord<?>> extends ItemWriter<T> {
    @Override
    void write(List<? extends T> items);

    Query getQuery(T record);
}
