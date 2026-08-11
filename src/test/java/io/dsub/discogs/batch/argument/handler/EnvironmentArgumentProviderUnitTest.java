package io.dsub.discogs.batch.argument.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvironmentArgumentProviderUnitTest {

  @Test
  void mapsEveryPublicEnvironmentVariable() {
    EnvironmentArgumentProvider provider =
        new EnvironmentArgumentProvider(
            Map.of(
                "OPEN_DISCOGS_BATCH_DATABASE_URL", "postgresql://user:pass@db:5432/discogs",
                "OPEN_DISCOGS_BATCH_DATABASE_SCHEMA", "open_discogs",
                "OPEN_DISCOGS_BATCH_ENTITIES", "artist,release",
                "OPEN_DISCOGS_BATCH_DUMP_MONTH", "2026-07",
                "OPEN_DISCOGS_BATCH_DATA_DIR", "/data",
                "OPEN_DISCOGS_BATCH_CHUNK_SIZE", "9000",
                "OPEN_DISCOGS_BATCH_MAX_WORKERS", "6",
                "OPEN_DISCOGS_BATCH_CLEANUP", "true",
                "OPEN_DISCOGS_BATCH_FORCE", "1",
                "OPEN_DISCOGS_BATCH_ALLOW_DOWNGRADE", "yes"));

    assertThat(provider.apply(new String[0]))
        .containsExactlyInAnyOrder(
            "--database-url=postgresql://user:pass@db:5432/discogs",
            "--database-schema=open_discogs",
            "--entities=artist,release",
            "--dump-month=2026-07",
            "--data-dir=/data",
            "--chunk-size=9000",
            "--max-workers=6",
            "--cleanup",
            "--force",
            "--allow-downgrade");
  }

  @Test
  void commandLineTakesPrecedenceAndFalseFlagsAreOmitted() {
    EnvironmentArgumentProvider provider =
        new EnvironmentArgumentProvider(
            Map.of(
                "OPEN_DISCOGS_BATCH_CHUNK_SIZE", "9000",
                "OPEN_DISCOGS_BATCH_MAX_WORKERS", "8",
                "OPEN_DISCOGS_BATCH_CLEANUP", "false"));

    assertThat(provider.apply(new String[] {"--chunk-size=1000", "--max-workers=2"}))
        .containsExactly("--chunk-size=1000", "--max-workers=2");
  }

  @Test
  void invalidBooleanFailsClearly() {
    EnvironmentArgumentProvider provider =
        new EnvironmentArgumentProvider(Map.of("OPEN_DISCOGS_BATCH_CLEANUP", "sometimes"));

    assertThatThrownBy(() -> provider.apply(new String[0]))
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessageContaining("OPEN_DISCOGS_BATCH_CLEANUP must be a boolean");
  }

  @Test
  void blankEnvironmentValuesAreIgnored() {
    EnvironmentArgumentProvider provider =
        new EnvironmentArgumentProvider(Map.of("OPEN_DISCOGS_BATCH_DATA_DIR", "  "));

    assertThat(provider.apply(new String[0])).isEmpty();
  }
}
