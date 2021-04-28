package io.dsub.discogsdata.batch.chunk.processor;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import io.dsub.discogsdata.batch.xml.XmlEntity;
import io.dsub.discogsdata.batch.xml.XmlRelation;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.task.TaskExecutor;

import java.util.Collection;

public interface XmlItemProcessorProvider {
    <S extends XmlEntity<T>, T extends BaseDTO> AsyncItemProcessor<S, T> buildEntityProcessor(TaskExecutor taskExecutor) throws Exception;
    <S extends XmlRelation<T>, T extends BaseDTO> AsyncItemProcessor<S, Collection<T>> buildRelationProcessor(TaskExecutor taskExecutor) throws Exception;
    <S extends XmlEntity<T>, T extends BaseDTO> ItemProcessor<S, T> buildEntityProcessor() throws Exception;
    <S extends XmlRelation<T>, T extends BaseDTO> ItemProcessor<S, Collection<T>> buildRelationProcessor() throws Exception;
}
