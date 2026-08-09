package io.dsub.discogs.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
}
