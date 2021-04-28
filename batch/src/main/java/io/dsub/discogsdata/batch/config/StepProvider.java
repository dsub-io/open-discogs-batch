package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import io.dsub.discogsdata.batch.dto.RelationalDTO;
import io.dsub.discogsdata.batch.xml.XmlEntity;
import io.dsub.discogsdata.batch.xml.XmlRelation;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;

public interface StepProvider {
    <S extends XmlRelation<T>, T extends RelationalDTO> Step buildRelationalDtoFlowStep(Class<S> sourceClass, Class<T> targetClass, String eTag, boolean isAsync) throws Exception;

    <S extends XmlRelation<T>, T extends RelationalDTO> Flow buildRelationalDtoFlow(Class<S> sourceClass, Class<T> targetClass, String eTag, boolean isAsync) throws Exception;

    <S extends XmlRelation<T>, T extends RelationalDTO> Step buildAsyncRelationalEntityInsertStep(Class<S> sourceClass, Class<T> targetClass, String eTag) throws Exception;

    <S extends XmlEntity<T>, T extends BaseDTO> Step buildAsyncEntityInsertStep(Class<S> sourceClass, Class<T> targetClass, String eTag) throws Exception;

    <S extends XmlEntity<T>, T extends BaseDTO> Step buildEntityInsertStep(Class<S> sourceClass, Class<T> targetClass, String eTag) throws Exception;

    <S extends XmlRelation<T>, T extends BaseDTO> Step buildRelationalEntityInsertStep(Class<S> sourceClass, Class<T> targetClass, String eTag) throws Exception;
}
