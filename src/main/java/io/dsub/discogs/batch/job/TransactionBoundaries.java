package io.dsub.discogs.batch.job;

import java.util.Objects;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/** Defines transaction propagation used by CPU-only batch preparation. */
public final class TransactionBoundaries {

  private static final String PREPARATION_NAME = "batch-cpu-preparation";

  public static TransactionOperations suspended(PlatformTransactionManager transactionManager) {
    Objects.requireNonNull(transactionManager, "transactionManager must not be null");
    DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
    definition.setName(PREPARATION_NAME);
    definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    return new TransactionTemplate(transactionManager, definition);
  }

  private TransactionBoundaries() {
  }
}
