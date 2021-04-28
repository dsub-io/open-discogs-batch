package io.dsub.discogsdata.batch.chunk.processor;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import io.dsub.discogsdata.batch.xml.XmlEntity;
import io.dsub.discogsdata.batch.xml.XmlRelation;
import lombok.Setter;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class DefaultXmlItemProcessorProvider implements XmlItemProcessorProvider, InitializingBean {

    @Setter
    private TaskExecutor taskExecutor;

    public DefaultXmlItemProcessorProvider(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        assert (taskExecutor != null);
    }

    @Override
    public <S extends XmlEntity<T>, T extends BaseDTO> AsyncItemProcessor<S, T> buildEntityProcessor(TaskExecutor taskExecutor) throws Exception {
        ItemProcessor<S, T> delegate = XmlEntity::toEntity;
        AsyncItemProcessor<S, T> processor = new AsyncItemProcessor<>();
        processor.setDelegate(delegate);
        processor.setTaskExecutor(taskExecutor);
        processor.afterPropertiesSet();
        return processor;
    }

    @Override
    public <S extends XmlRelation<T>, T extends BaseDTO> AsyncItemProcessor<S, Collection<T>> buildRelationProcessor(TaskExecutor taskExecutor) throws Exception {
        ItemProcessor<S, Collection<T>> delegate = XmlRelation::toEntities;
        AsyncItemProcessor<S, Collection<T>> processor = new AsyncItemProcessor<>();
        processor.setDelegate(delegate);
        processor.setTaskExecutor(taskExecutor);
        processor.afterPropertiesSet();
        return processor;
    }

    @Override
    public <S extends XmlEntity<T>, T extends BaseDTO> ItemProcessor<S, T> buildEntityProcessor() {
        return XmlEntity::toEntity;
    }

    @Override
    public <S extends XmlRelation<T>, T extends BaseDTO> ItemProcessor<S, Collection<T>> buildRelationProcessor() {
        return xmlRelation -> xmlRelation.toEntities()
                .parallelStream()
                .collect(Collectors.toList());
    }
}
