package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.dump.DumpItem;
import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.entity.label.dto.LabelDTO;
import io.dsub.discogsdata.batch.entity.label.dto.LabelSubLabelDTO;
import io.dsub.discogsdata.batch.entity.label.dto.LabelUrlDTO;
import io.dsub.discogsdata.batch.entity.label.xml.XmlLabel;
import io.dsub.discogsdata.batch.entity.label.xml.XmlLabelSubLabel;
import io.dsub.discogsdata.batch.entity.label.xml.XmlLabelUrl;
import io.dsub.discogsdata.batch.step.FileFetchStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LabelStepProviderImpl {

    private final StepProvider stepProvider;
    private final DumpService dumpService;
    private final JobRepository jobRepository;

    @Bean
    @JobScope
    public Step labelStep(@Value("#{jobParameters['label']}") String eTag) throws Exception {

        Step labelInsertStep = stepProvider.buildEntityInsertStep(XmlLabel.class, LabelDTO.class, eTag);
        Step labelSubLabelStep = stepProvider.buildRelationalDtoFlowStep(XmlLabelSubLabel.class, LabelSubLabelDTO.class, eTag, false);
        Step labelUrlStep = stepProvider.buildRelationalDtoFlowStep(XmlLabelUrl.class, LabelUrlDTO.class, eTag, false);

        DumpItem dump = dumpService.getDumpByEtag(eTag);
        Flow flow = new FlowBuilder<SimpleFlow>("labelFlow")
                .start(new FileFetchStep(dump))
                .next(labelInsertStep)
                .next(labelSubLabelStep)
                .next(labelUrlStep)
//                .next(new FileCleanupStep(dump))
                .build();

        FlowStep flowStep = new FlowStep();
        flowStep.setFlow(flow);
        flowStep.setName("label step");
        flowStep.setStartLimit(Integer.MAX_VALUE);
        flowStep.setJobRepository(jobRepository);
        flowStep.afterPropertiesSet();
        return flowStep;
    }
}
