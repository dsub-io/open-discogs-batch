package io.dsub.discogsdata.batch.chunk.writer;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemWriter;

public interface DtoWriterProvider {
    <T extends BaseDTO> AsyncItemWriter<T> buildAsyncDtoWriter(Class<T> clazz, boolean updateIfExists) throws Exception;
    <T extends BaseDTO> ItemWriter<T> buildDtoWriter(Class<T> clazz, boolean updateIfExists) throws Exception;
    <T extends BaseDTO> AsyncCollectionItemWriter<T> buildAsyncDtoCollectionWriter(Class<T> clazz, boolean updateIfExists) throws Exception;
    <T extends BaseDTO> CollectionItemWriter<T> buildCollectionWriter(Class<T> clazz, boolean updateIfExists) throws Exception;
}