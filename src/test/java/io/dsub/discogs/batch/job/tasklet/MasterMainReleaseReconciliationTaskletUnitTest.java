package io.dsub.discogs.batch.job.tasklet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.job.reconciliation.MasterMainReleaseReconciler;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

class MasterMainReleaseReconciliationTaskletUnitTest {

  @Test
  void delegatesOneReconciliationAndFinishes() {
    MasterMainReleaseReconciler reconciler = mock(MasterMainReleaseReconciler.class);
    MasterMainReleaseReconciliationTasklet tasklet =
        new MasterMainReleaseReconciliationTasklet(reconciler);

    assertThat(tasklet.execute(null, null)).isEqualTo(RepeatStatus.FINISHED);
    verify(reconciler).reconcile();
  }
}
