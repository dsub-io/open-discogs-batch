package io.dsub.discogs.batch.job;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ContextConfiguration;

/**
 * Runs the complete deterministic dump-to-PostgreSQL path in the required E2E lane.
 *
 * <p>The inherited scenarios cover all entity types, selective imports, exact business-state
 * idempotency, and the shared cross-language golden state.
 */
@Tag("e2e")
@ContextConfiguration(classes = PostgreSQLIntegrationTestConfig.class)
public class PostgreSQLDiscogsJobE2ETest extends DiscogsJobIntegrationTest {

}
