package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.junit.jupiter.api.Test;

class BlockingTaskSubmissionPolicyUnitTest {

  @Test
  void appliesBackpressureUntilAWorkerIsAvailable() throws Exception {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            new BlockingTaskSubmissionPolicy());
    CountDownLatch firstStarted = new CountDownLatch(1);
    CountDownLatch releaseFirst = new CountDownLatch(1);
    CountDownLatch submissionStarted = new CountDownLatch(1);
    CountDownLatch secondCompleted = new CountDownLatch(1);
    try (var submitter = Executors.newSingleThreadExecutor()) {
      executor.execute(
          () -> {
            firstStarted.countDown();
            try {
              releaseFirst.await();
            } catch (InterruptedException exception) {
              Thread.currentThread().interrupt();
            }
          });
      assertThat(firstStarted.await(1, TimeUnit.SECONDS)).isTrue();

      Future<?> blockedSubmission =
          submitter.submit(
              () -> {
                submissionStarted.countDown();
                executor.execute(secondCompleted::countDown);
              });

      assertThat(submissionStarted.await(1, TimeUnit.SECONDS)).isTrue();
      assertThat(blockedSubmission).isNotDone();
      assertThat(secondCompleted.getCount()).isEqualTo(1);
      releaseFirst.countDown();
      assertThat(blockedSubmission).succeedsWithin(Duration.ofSeconds(1)).isNull();
      assertThat(secondCompleted.await(1, TimeUnit.SECONDS)).isTrue();
    } finally {
      releaseFirst.countDown();
      executor.shutdownNow();
    }
  }

  @Test
  void rejectsSubmissionWhenExecutorIsShuttingDown() {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
    executor.shutdown();

    assertThatThrownBy(
            () -> new BlockingTaskSubmissionPolicy().rejectedExecution(() -> {}, executor))
        .isInstanceOf(RejectedExecutionException.class)
        .hasMessage("import worker pool is shutting down");
  }

  @Test
  void preservesInterruptWhenBlockedSubmissionIsInterrupted() {
    ThreadPoolExecutor executor =
        new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(
              () -> new BlockingTaskSubmissionPolicy().rejectedExecution(() -> {}, executor))
          .isInstanceOf(RejectedExecutionException.class)
          .hasMessage("interrupted while waiting for an import worker");
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
      executor.shutdownNow();
    }
  }

  @Test
  void retriesAfterTimedQueueOfferFails() throws Exception {
    ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
    @SuppressWarnings("unchecked")
    BlockingQueue<Runnable> queue = mock(BlockingQueue.class);
    Runnable task = () -> {};
    when(executor.isShutdown()).thenReturn(false, true);
    when(executor.getQueue()).thenReturn(queue);
    when(queue.offer(eq(task), eq(100L), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

    assertThatThrownBy(() -> new BlockingTaskSubmissionPolicy().rejectedExecution(task, executor))
        .isInstanceOf(RejectedExecutionException.class)
        .hasMessage("import worker pool is shutting down");
  }
}
