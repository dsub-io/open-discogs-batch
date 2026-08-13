package io.dsub.discogs.batch.job;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = PostgreSQLIntegrationTestConfig.class)
public class PostgreSQLDiscogsJobIntegrationTest extends DiscogsJobIntegrationTest {

  @Test
  void relationFailureFailsTheWholeJobInsteadOfEndingSuccessfully() throws Exception {
    runRelationFailurePropagationScenario();
  }

  @Test
  void failedMultiEntityRunResumesCompletedSourceChunksWithoutRewritingThem() throws Exception {
    runMultiEntityResumeScenario();
  }

  @Test
  void retryReconcilesBacklinksWithoutRewritingCommittedReleaseChunks() throws Exception {
    runSeparatedReleaseReconciliationRetryScenario();
  }
}
