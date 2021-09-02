package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.job.step.core.ArtistStepConfig;
import io.dsub.discogs.batch.job.step.core.LabelStepConfig;
import io.dsub.discogs.batch.job.step.core.MasterStepConfig;
import io.dsub.discogs.batch.job.step.core.ReleaseItemStepConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

@Slf4j
@RequiredArgsConstructor
public class CacheInversionStepExecutionListener implements StepExecutionListener {

  private static final String INVERT_CACHE_MSG = "inverting {} id cache";
  private static final String ARTIST = "artist";
  private static final String LABEL = "label";
  private static final String MASTER = "master";
  private static final String RELEASE_ITEM = "release item";
  private final EntityIdRegistry idRegistry;

  @Override
  public void beforeStep(StepExecution stepExecution) {
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    boolean doMaster = stepExecution.getJobParameters().getParameters().containsKey(MASTER);
    boolean doRelease = stepExecution.getJobParameters().getParameters().containsKey(RELEASE_ITEM);

    // current
    String stepName = stepExecution.getStepName();

    if (stepName.equals(ArtistStepConfig.ARTIST_CORE_INSERTION_STEP) && (doMaster || doRelease)) {
      log.info(INVERT_CACHE_MSG, ARTIST);
      idRegistry.invert(DefaultEntityIdRegistry.Type.ARTIST);
    }

    if (stepName.equals(LabelStepConfig.LABEL_CORE_INSERTION_STEP) && (doMaster || doRelease)) {
      log.info(INVERT_CACHE_MSG, LABEL);
      idRegistry.invert(DefaultEntityIdRegistry.Type.LABEL);
    }

    if (stepName.equals(MasterStepConfig.MASTER_CORE_INSERTION_STEP) && doRelease) {
      log.info(INVERT_CACHE_MSG, MASTER);
      idRegistry.invert(DefaultEntityIdRegistry.Type.MASTER);
    }

    if (stepName.equals(ReleaseItemStepConfig.RELEASE_ITEM_CORE_INSERTION_STEP) && (doMaster)) {
      log.info(INVERT_CACHE_MSG, RELEASE_ITEM);
      idRegistry.invert(DefaultEntityIdRegistry.Type.RELEASE);
    }

    return stepExecution.getExitStatus();
  }
}
