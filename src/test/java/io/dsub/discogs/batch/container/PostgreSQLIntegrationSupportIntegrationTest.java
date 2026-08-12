package io.dsub.discogs.batch.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PostgreSQLIntegrationSupportIntegrationTest extends PostgreSQLIntegrationSupport {

  @Test
  void ownsAnEphemeralPostgreSqlContainerForThisTestRun() {
    var containerInfo = CONTAINER.getContainerInfo();

    assertDoesNotThrow(() -> verifyContainerConfiguration(containerInfo));
    assertEquals(TEST_OWNER,
        containerInfo.getConfig().getLabels().get(TEST_OWNER_LABEL));
    assertEquals(TEST_RUN_ID,
        containerInfo.getConfig().getLabels().get(TEST_RUN_LABEL));
    assertEquals(Map.of(POSTGRES_DATA_DIRECTORY, POSTGRES_TMPFS_OPTIONS),
        containerInfo.getHostConfig().getTmpFs());
    assertFalse(CONTAINER.isShouldBeReused());
  }
}
