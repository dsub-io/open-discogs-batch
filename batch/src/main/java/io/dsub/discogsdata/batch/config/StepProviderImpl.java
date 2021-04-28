package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.chunk.processor.XmlItemProcessorProvider;
import io.dsub.discogsdata.batch.chunk.writer.DtoWriterProvider;
import io.dsub.discogsdata.batch.dto.BaseDTO;
import io.dsub.discogsdata.batch.dto.DtoQueryBuilder;
import io.dsub.discogsdata.batch.dto.RelationalDTO;
import io.dsub.discogsdata.batch.dump.DumpItem;
import io.dsub.discogsdata.batch.dump.DumpItemReaderProvider;
import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.step.QueryStepProvider;
import io.dsub.discogsdata.batch.xml.XmlEntity;
import io.dsub.discogsdata.batch.xml.XmlRelation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.TaskExecutor;

import java.util.Collection;
import java.util.concurrent.Future;

@Setter
@Getter
@RequiredArgsConstructor
public class StepProviderImpl implements StepProvider, InitializingBean {

    private final JobRepository jobRepository;
    private final QueryStepProvider queryStepProvider;
    private final StepBuilderFactory stepBuilderFactory;
    private final DumpItemReaderProvider dumpItemReaderProvider;
    private final XmlItemProcessorProvider xmlItemProcessorProvider;
    private final DtoWriterProvider dtoWriterProvider;
    private final DumpService dumpService;
    private final TaskExecutor taskExecutor;
    private final DtoQueryBuilder queryBuilder;

    @Override
    public void afterPropertiesSet() {
        assert (jobRepository != null);
        assert (queryStepProvider != null);
        assert (stepBuilderFactory != null);
        assert (dumpItemReaderProvider != null);
        assert (xmlItemProcessorProvider != null);
        assert (dumpService != null);
        assert (taskExecutor != null);
        assert (dtoWriterProvider != null);
    }

    @Override
    public <S extends XmlRelation<T>, T extends RelationalDTO> Step buildRelationalDtoFlowStep(Class<S> sourceClass, Class<T> targetClass, String eTag, boolean isAsync) throws Exception {
        FlowStep flowStep = new FlowStep();
        flowStep.setFlow(buildRelationalDtoFlow(sourceClass, targetClass, eTag, isAsync));
        flowStep.setJobRepository(jobRepository);
        flowStep.setAllowStartIfComplete(true);
        flowStep.setStartLimit(Integer.MAX_VALUE);
        flowStep.setName(targetClass.getSimpleName() + "Step");
        flowStep.afterPropertiesSet();
        return flowStep;
    }

    @Override
    public <S extends XmlRelation<T>, T extends RelationalDTO> Flow buildRelationalDtoFlow(Class<S> sourceClass, Class<T> targetClass, String eTag, boolean isAsync) throws Exception {
        String targetName = targetClass.getSimpleName();
        String flowName = targetName + "Flow";
        String createQuery = queryBuilder.buildCreateCloneTableQuery(targetClass);
        String dropQuery = queryBuilder.buildDropCloneTableQuery(targetClass);
        String injectQuery = queryBuilder.buildInjectCloneTableQuery(targetClass);
        String pruneQuery = queryBuilder.buildComparePruneCloneTableQuery(targetClass);
        return new FlowBuilder<SimpleFlow>(flowName)
                .start(queryStepProvider.get(targetName + " create temporary table", dropQuery, createQuery))
                .next(buildAsyncRelationalEntityInsertStep(sourceClass, targetClass, eTag))
                .next(queryStepProvider.get(targetName + " compare then prune original table", pruneQuery))
                .next(queryStepProvider.get(targetName + " inject temporary table value", injectQuery))
                .next(queryStepProvider.get(targetName + " drop temporary table value", dropQuery))
                .build();
    }

    @Override
    public <S extends XmlRelation<T>, T extends RelationalDTO> Step
    buildAsyncRelationalEntityInsertStep(Class<S> sourceClass,
                                         Class<T> targetClass,
                                         String eTag) throws Exception {

        return stepBuilderFactory.get("asyncChunkInsertStep[" + targetClass.getSimpleName() + "] " + eTag)
                .<S, Future<Collection<T>>>chunk(io.dsub.discogsdata.batch.config.AppConfig.CHUNK_SIZE)
                .reader(dumpItemReaderProvider.getReaderFrom(sourceClass, dumpService.getDumpByEtag(eTag), "reading [" + sourceClass.getSimpleName() + "]"))
                .processor(xmlItemProcessorProvider.buildRelationProcessor(taskExecutor))
                .writer(null)
                .build();
    }

    @Override
    public <S extends XmlEntity<T>, T extends BaseDTO> Step
    buildAsyncEntityInsertStep(Class<S> sourceClass,
                               Class<T> targetClass,
                               String eTag) throws Exception {
        return stepBuilderFactory.get("asyncEntityInsertStep[" + targetClass.getSimpleName() + "] " + eTag)
                .<S, Future<T>>chunk(io.dsub.discogsdata.batch.config.AppConfig.CHUNK_SIZE)
                .reader(dumpItemReaderProvider.getReaderFrom(sourceClass, dumpService.getDumpByEtag(eTag), "reading [" + sourceClass.getSimpleName() + "]"))
                .processor(xmlItemProcessorProvider.buildEntityProcessor(taskExecutor))
                .writer(dtoWriterProvider.buildAsyncDtoWriter(targetClass, true))
                .build();
    }

    @Override
    public <S extends XmlEntity<T>, T extends BaseDTO> Step buildEntityInsertStep(Class<S> sourceClass,
                                                                                  Class<T> targetClass,
                                                                                  String eTag) throws Exception {
        final String readerTaskName = "reading [" + sourceClass.getSimpleName() + "]";
        DumpItem dump = dumpService.getDumpByEtag(eTag);
        return stepBuilderFactory.get("entityInsertStep[" + targetClass.getSimpleName() + "] " + eTag)
                .<S, T>chunk(io.dsub.discogsdata.batch.config.AppConfig.CHUNK_SIZE)
                .reader(dumpItemReaderProvider.getReaderFrom(sourceClass, dump, readerTaskName))
                .processor(xmlItemProcessorProvider.buildEntityProcessor())
                .writer(dtoWriterProvider.buildDtoWriter(targetClass, true))
                .taskExecutor(taskExecutor)
                .throttleLimit(io.dsub.discogsdata.batch.config.AppConfig.THROTTLE_LIMIT)
                .build();
    }

    @Override
    public <S extends XmlRelation<T>, T extends BaseDTO> Step buildRelationalEntityInsertStep(Class<S> sourceClass,
                                                                                            Class<T> targetClass,
                                                                                            String eTag) throws Exception {
        final String readerTaskName = "reading [" + sourceClass.getSimpleName() + "]";
        DumpItem dump = dumpService.getDumpByEtag(eTag);

        return stepBuilderFactory.get("entityInsertStep[" + targetClass.getSimpleName() + "] " + eTag)
                .<S, Collection<T>>chunk(io.dsub.discogsdata.batch.config.AppConfig.CHUNK_SIZE)
                .reader(dumpItemReaderProvider.getReaderFrom(sourceClass, dump, readerTaskName))
                .processor(xmlItemProcessorProvider.buildRelationProcessor())
                .writer(dtoWriterProvider.buildCollectionWriter(targetClass, false))
                .taskExecutor(taskExecutor)
                .throttleLimit(io.dsub.discogsdata.batch.config.AppConfig.THROTTLE_LIMIT)
                .build();
    }
}
