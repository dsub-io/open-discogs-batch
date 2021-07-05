package io.dsub.discogs.batch.job.step;

import io.dsub.discogs.batch.job.step.core.ArtistStepConfig;
import io.dsub.discogs.batch.job.step.core.LabelStepConfig;
import io.dsub.discogs.batch.job.step.core.MasterStepConfig;
import io.dsub.discogs.batch.job.step.core.ReleaseItemStepConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(
    value = {
        ArtistStepConfig.class,
        LabelStepConfig.class,
        MasterStepConfig.class,
        ReleaseItemStepConfig.class
    })
public class GlobalStepConfig {

}
