package io.dsub.discogsdata.batch.config;

import io.dsub.discogsdata.batch.dump.DumpItem;
import io.dsub.discogsdata.batch.dump.DumpService;
import io.dsub.discogsdata.batch.entity.artist.dto.*;
import io.dsub.discogsdata.batch.entity.artist.xml.*;
import io.dsub.discogsdata.batch.step.FileCleanupStep;
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
public class ArtistStepProviderImpl {

    private final StepProvider stepProvider;
    private final DumpService dumpService;
    private final JobRepository jobRepository;

    @Bean
    @JobScope
    public Step artistStep(@Value("#{jobParameters['artist']}") String eTag) throws Exception {
        Step artistInsertStep = stepProvider.buildAsyncEntityInsertStep(XmlArtist.class, ArtistDTO.class, eTag);
        Step artistAliasStep = stepProvider.buildRelationalDtoFlowStep(XmlArtistAlias.class, ArtistAliasDTO.class, eTag, false);
        Step artistMemberStep = stepProvider.buildRelationalDtoFlowStep(XmlArtistMember.class, ArtistMemberDTO.class, eTag, false);
        Step artistGroupStep = stepProvider.buildRelationalDtoFlowStep(XmlArtistGroup.class, ArtistGroupDTO.class, eTag, false);
        Step artistNameVariationStep = stepProvider.buildRelationalDtoFlowStep(XmlArtistNameVariation.class, ArtistNameVariationDTO.class, eTag, false);
        Step artistUrlStep = stepProvider.buildRelationalDtoFlowStep(XmlArtistUrl.class, ArtistUrlDTO.class, eTag, false);
        DumpItem dump = dumpService.getDumpByEtag(eTag);

        Flow flow =  new FlowBuilder<SimpleFlow>("artistFlow")
                .start(new FileFetchStep(dump))
                .next(artistInsertStep)
                .next(artistAliasStep)
                .next(artistGroupStep)
                .next(artistMemberStep)
                .next(artistNameVariationStep)
                .next(artistUrlStep)
                .next(new FileCleanupStep(dump))
                .build();

        FlowStep flowStep = new FlowStep();
        flowStep.setJobRepository(jobRepository);
        flowStep.setName("artist step");
        flowStep.setStartLimit(Integer.MAX_VALUE);
        flowStep.setFlow(flow);
        flowStep.afterPropertiesSet();
        return flowStep;
    }
}
