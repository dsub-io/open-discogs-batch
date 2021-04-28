package io.dsub.discogsdata.batch.chunk.writer;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.util.List;

public class DtoJdbcItemWriter<T extends BaseDTO> implements ItemWriter<T>, InitializingBean {

    private final JdbcBatchItemWriter<T> delegate;

    public DtoJdbcItemWriter(DataSource dataSource, String query) throws Exception {
        this.delegate = new JdbcBatchItemWriterBuilder<T>()
                .assertUpdates(false)
                .sql(query)
                .beanMapped()
                .dataSource(dataSource)
                .build();
        this.afterPropertiesSet();
    }

    @Override
    public void write(List<? extends T> items) throws Exception {
        delegate.write(items);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        assert (delegate != null);
        delegate.afterPropertiesSet();
    }
}
