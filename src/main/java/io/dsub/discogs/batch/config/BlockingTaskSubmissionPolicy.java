package io.dsub.discogs.batch.config;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Applies producer backpressure when all import workers are busy. */
final class BlockingTaskSubmissionPolicy implements RejectedExecutionHandler {

  private static final long SHUTDOWN_CHECK_INTERVAL_MILLIS = 100;

  @Override
  public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
    try {
      while (!executor.isShutdown()) {
        if (executor
            .getQueue()
            .offer(task, SHUTDOWN_CHECK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)) {
          return;
        }
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RejectedExecutionException("interrupted while waiting for an import worker", exception);
    }
    throw new RejectedExecutionException("import worker pool is shutting down");
  }
}
