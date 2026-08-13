package io.dsub.discogs.batch.job.tasklet;

import io.dsub.discogs.batch.job.reconciliation.MasterMainReleaseReconciler;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/** Runs backlink reconciliation only after all Release root and relation chunks commit. */
@Component
public final class MasterMainReleaseReconciliationTasklet implements Tasklet {

  private final MasterMainReleaseReconciler reconciler;

  public MasterMainReleaseReconciliationTasklet(MasterMainReleaseReconciler reconciler) {
    this.reconciler = reconciler;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    reconciler.reconcile();
    return RepeatStatus.FINISHED;
  }
}
