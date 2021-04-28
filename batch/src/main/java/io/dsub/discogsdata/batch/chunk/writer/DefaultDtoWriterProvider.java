package io.dsub.discogsdata.batch.chunk.writer;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import io.dsub.discogsdata.batch.dto.DtoQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@RequiredArgsConstructor
public class DefaultDtoWriterProvider implements DtoWriterProvider, InitializingBean {

    private final DataSource dataSource;
    private final DtoQueryBuilder dtoQueryBuilder;

    @Override
    public void afterPropertiesSet() throws Exception {
        assert (dataSource != null);
    }

    @Override
    public <T extends BaseDTO> AsyncItemWriter<T> buildAsyncDtoWriter(Class<T> clazz, boolean updateIfExists) throws Exception  {
        final String query = dtoQueryBuilder.buildInsertQuery(clazz, updateIfExists);
        DtoJdbcItemWriter<T> delegate = new DtoJdbcItemWriter<>(dataSource, query);
        delegate.afterPropertiesSet();
        AsyncItemWriter<T> writer = new AsyncItemWriter<>();
        writer.setDelegate(delegate);
        writer.afterPropertiesSet();
        return writer;
    }

    @Override
    public <T extends BaseDTO> AsyncCollectionItemWriter<T> buildAsyncDtoCollectionWriter(Class<T> clazz, boolean updateIfExists) throws Exception {
        final String query = dtoQueryBuilder.buildInsertQuery(clazz, updateIfExists);
        DtoJdbcItemWriter<T> delegate = new DtoJdbcItemWriter<>(dataSource, query);
        AsyncCollectionItemWriter<T> writer = new AsyncCollectionItemWriter<>();
        writer.setDelegate(delegate);
        writer.afterPropertiesSet();
        return writer;
    }

    @Override
    public <T extends BaseDTO> ItemWriter<T> buildDtoWriter(Class<T> clazz, boolean updateIfExists) throws Exception {
        final String query = dtoQueryBuilder.buildInsertQuery(clazz, updateIfExists);
        DtoJdbcItemWriter<T> writer = new DtoJdbcItemWriter<T>(dataSource, query);
        writer.afterPropertiesSet();
        return writer;
    }

    @Override
    public <T extends BaseDTO> CollectionItemWriter<T> buildCollectionWriter(Class<T> clazz, boolean updateIfExists) throws Exception {
        final String query = dtoQueryBuilder.buildInsertQuery(clazz, updateIfExists);
        CollectionItemWriter<T> wrapper = new CollectionItemWriter<>();
        DtoJdbcItemWriter<T> delegate = new DtoJdbcItemWriter<>(dataSource, query);
        wrapper.setDelegate(delegate);
        wrapper.afterPropertiesSet();
        return wrapper;
    }
}
