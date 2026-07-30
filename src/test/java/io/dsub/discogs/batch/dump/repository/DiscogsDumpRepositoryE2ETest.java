package io.dsub.discogs.batch.dump.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.testutil.DiscogsDumpE2EFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

@Tag("e2e")
class DiscogsDumpRepositoryE2ETest {

  static DumpSupplier dumpSupplier;
  static DiscogsDumpRepository repository;

  @BeforeAll
  static void beforeAll() throws Exception {
    dumpSupplier = Mockito.mock(DumpSupplier.class);
    Mockito.when(dumpSupplier.get()).thenReturn(DiscogsDumpE2EFixture.getDumps());
    MapDiscogsDumpRepository mapDiscogsDumpRepository = new MapDiscogsDumpRepository(dumpSupplier);
    mapDiscogsDumpRepository.afterPropertiesSet();
    repository = mapDiscogsDumpRepository;
  }

  @Test
  void whenFindAll__ShouldNotReturnEmptyList() {
    // when
    List<DiscogsDump> found = repository.findAll();

    // then
    assertThat(found).isNotEmpty();
  }

  @ParameterizedTest
  @EnumSource(EntityType.class)
  void whenFindTopByType__ShouldReturnDiscogsDumpWithValidValues(EntityType type) {
    // when
    DiscogsDump dump = repository.findTopByType(type);

    // then
    assertAll(
        () -> assertThat(dump.getLastModifiedAt()).isNotNull(),
        () -> assertThat(dump.getETag()).isNotNull(),
        () -> assertThat(dump.getSize()).isNotNull(),
        () -> assertThat(dump.getUriString()).isNotNull(),
        () -> assertThat(dump.getFileName()).isNotNull(),
        () -> assertThat(dump.getType()).isNotNull(),
        () -> assertThat(dump.getUrl()).isNotNull(),
        () -> assertThat(dump.getChecksumUrl()).isNotNull());
  }
}
