package io.dsub.discogs.batch.job;

import java.time.Duration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.PessimisticLockingFailureException;

/** Bounded retry settings for transient PostgreSQL lock contention. */
public final class BatchRetryPolicy {

  static final int LOCK_RETRY_LIMIT = 5;
  static final long INITIAL_BACKOFF_MILLIS = 100;
  static final long BACKOFF_JITTER_MILLIS = 50;
  static final long MAX_BACKOFF_MILLIS = 2_000;
  static final double BACKOFF_MULTIPLIER = 2.0;

  public static RetryPolicy lockContention() {
    return RetryPolicy.builder()
        .maxRetries(LOCK_RETRY_LIMIT)
        .delay(Duration.ofMillis(INITIAL_BACKOFF_MILLIS))
        .jitter(Duration.ofMillis(BACKOFF_JITTER_MILLIS))
        .multiplier(BACKOFF_MULTIPLIER)
        .maxDelay(Duration.ofMillis(MAX_BACKOFF_MILLIS))
        .includes(PessimisticLockingFailureException.class)
        .build();
  }

  private BatchRetryPolicy() {
  }
}
