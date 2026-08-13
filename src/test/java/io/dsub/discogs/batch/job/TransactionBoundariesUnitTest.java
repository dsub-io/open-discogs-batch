package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionBoundariesUnitTest {

  @Test
  void suspendsDatabaseTransactionsForCpuPreparation() {
    RecordingTransactionManager manager = new RecordingTransactionManager();
    TransactionTemplate boundary = (TransactionTemplate) TransactionBoundaries.suspended(manager);

    String result = boundary.execute(ignored -> "prepared");

    assertThat(result).isEqualTo("prepared");
    assertThat(boundary.getPropagationBehavior())
        .isEqualTo(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
  }

  @Test
  void rejectsMissingTransactionManager() {
    assertThatThrownBy(() -> TransactionBoundaries.suspended(null))
        .isInstanceOf(NullPointerException.class);
  }

  private static final class RecordingTransactionManager
      extends AbstractPlatformTransactionManager {

    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
    }

    @Override
    protected void doCommit(DefaultTransactionStatus status) {
    }

    @Override
    protected void doRollback(DefaultTransactionStatus status) {
    }
  }
}
