package io.dsub.discogs.batch.job.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class IdCacheUnitTest {

  @Test
  void storesPositiveIdentifiersAcrossSegments() {
    IdCache cache = new IdCache(DefaultEntityIdRegistry.Type.RELEASE);

    cache.add(1);
    cache.add(65_535);
    cache.add(65_536);
    cache.add(200_000_000);

    assertThat(cache.exists(1)).isTrue();
    assertThat(cache.exists(65_535)).isTrue();
    assertThat(cache.exists(65_536)).isTrue();
    assertThat(cache.exists(200_000_000)).isTrue();
    assertThat(cache.exists(2)).isFalse();
    assertThat(cache.allocatedWordBytes()).isEqualTo(3L * 8_192L);
  }

  @Test
  void ignoresInvalidAndDuplicateIdentifiers() {
    IdCache cache = new IdCache(DefaultEntityIdRegistry.Type.ARTIST);

    cache.add(null);
    cache.add(0);
    cache.add(-1);
    cache.add(42);
    cache.add(42);

    assertThat(cache.exists(null)).isFalse();
    assertThat(cache.exists(0)).isFalse();
    assertThat(cache.exists(-1)).isFalse();
    assertThat(cache.exists(42)).isTrue();
    assertThat(cache.allocatedWordBytes()).isEqualTo(8_192L);
  }

  @Test
  void supportsConcurrentRegistration() {
    IdCache cache = new IdCache(DefaultEntityIdRegistry.Type.MASTER);

    IntStream.rangeClosed(1, 100_000).parallel().forEach(cache::add);

    assertThat(cache.exists(1)).isTrue();
    assertThat(cache.exists(50_000)).isTrue();
    assertThat(cache.exists(100_000)).isTrue();
    assertThat(cache.exists(100_001)).isFalse();
  }

  @Test
  void clearsAllocatedSegments() {
    IdCache cache = new IdCache(DefaultEntityIdRegistry.Type.LABEL);
    cache.add(10);

    cache.clear();

    assertThat(cache.isEmpty()).isTrue();
    assertThat(cache.exists(10)).isFalse();
    assertThat(cache.allocatedWordBytes()).isZero();
  }
}
