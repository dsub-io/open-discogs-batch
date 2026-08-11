package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

/**
 * Runs the complete deterministic dump-to-PostgreSQL path in the required E2E lane.
 *
 * <p>The inherited scenarios cover all entity types, selective imports, exact business-state
 * idempotency, and the shared cross-language golden state.
 */
@Tag("e2e")
@ContextConfiguration(classes = PostgreSQLCustomSchemaTestConfig.class)
public class PostgreSQLDiscogsJobE2ETest extends DiscogsJobIntegrationTest {

  @Autowired private DataSource schemaDataSource;

  @Test
  void canonicalTablesAreIsolatedInTheSelectedSchema() {
    JdbcTemplate jdbc = new JdbcTemplate(schemaDataSource);

    assertThat(
            jdbc.queryForObject(
                "select to_regclass('open_discogs.artist') is not null", Boolean.class))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "select to_regclass('public.artist') is null", Boolean.class))
        .isTrue();
  }
}
