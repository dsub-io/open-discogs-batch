package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.util.backoff.BackOffExecution;

class BatchRetryPolicyUnitTest {

  @Test
  void lockRetryUsesBoundedExponentialRandomBackoff() {
    var policy = BatchRetryPolicy.lockContention();
    var execution = policy.getBackOff().start();
    List<Long> delays = new ArrayList<>();
    long delay;
    while ((delay = execution.nextBackOff()) != BackOffExecution.STOP) {
      delays.add(delay);
    }

    assertThat(policy.shouldRetry(new PessimisticLockingFailureException("locked"))).isTrue();
    assertThat(policy.shouldRetry(new IllegalStateException("not retryable"))).isFalse();
    assertThat(delays).hasSize(BatchRetryPolicy.LOCK_RETRY_LIMIT);
    assertThat(delays.get(0))
        .isBetween(
            BatchRetryPolicy.INITIAL_BACKOFF_MILLIS,
            BatchRetryPolicy.INITIAL_BACKOFF_MILLIS
                + BatchRetryPolicy.BACKOFF_JITTER_MILLIS);
    assertThat(delays).allMatch(value -> value <= BatchRetryPolicy.MAX_BACKOFF_MILLIS);
    assertThat(BatchRetryPolicy.lockContention()).isNotSameAs(policy);
  }
}
