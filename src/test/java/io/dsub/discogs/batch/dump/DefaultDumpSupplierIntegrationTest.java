package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.repository.MapDiscogsDumpRepository;
import io.dsub.discogs.batch.dump.service.DefaultDiscogsDumpService;
import io.dsub.discogs.batch.testutil.DiscogsDumpE2EFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultDumpSupplierIntegrationTest {

  @AfterAll
  static void afterAll() {
    DiscogsDumpE2EFixture.shutdown();
  }

  @Test
  void whenGet__ThenReturnsNotEmptyListOfValidDiscogsDumps() throws Exception {
    List<DiscogsDump> foundList = DiscogsDumpE2EFixture.getDumps();
    List<DiscogsDump> latestCompleteDumpSet = getLatestCompleteDumpSet(foundList);

    assertThat(foundList).isNotNull().isNotEmpty();
    assertThat(latestCompleteDumpSet)
        .hasSize(EntityType.values().length)
        .extracting(DiscogsDump::getType)
        .containsExactlyInAnyOrder(EntityType.values());
    assertThat(latestCompleteDumpSet)
        .extracting(DiscogsDump::getLastModifiedAt)
        .containsOnly(latestCompleteDumpSet.getFirst().getLastModifiedAt());
    foundList.forEach(
        item ->
            assertThat(item)
                .satisfies(dump -> assertThat(dump.getETag()).isNotNull().isNotBlank())
                .satisfies(dump -> assertThat(dump.getSize()).isPositive())
                .satisfies(dump -> assertThat(dump.getUriString()).isNotNull().isNotBlank())
                .satisfies(dump -> assertThat(dump.getFileName()).matches("^[\\w_]+\\.xml\\.gz$"))
                .satisfies(dump -> assertThat(dump.getChecksumUrl()).isNotNull())
                .satisfies(dump -> assertThat(dump.getType()).isNotNull()));
  }

  @Test
  void whenGetLatestCompleteDumpSet__ThenDownloadsVerifyAgainstSharedManifest(
      @TempDir Path tempDir) throws Exception {
    List<DiscogsDump> latestCompleteDumpSet =
        getLatestCompleteDumpSet(DiscogsDumpE2EFixture.getDumps());
    DiscogsDump firstDump = latestCompleteDumpSet.getFirst();

    assertThat(latestCompleteDumpSet)
        .extracting(DiscogsDump::getChecksumUrl)
        .containsOnly(firstDump.getChecksumUrl());
    assertThat(latestCompleteDumpSet)
        .extracting(DiscogsDump::getLastModifiedAt)
        .containsOnly(firstDump.getLastModifiedAt());

    DiscogsDumpVerifier verifier = new DiscogsDumpVerifier();
    for (DiscogsDump dump : latestCompleteDumpSet) {
      Path downloadedFile = tempDir.resolve(dump.getFileName());
      try (var input = dump.getInputStream()) {
        Files.copy(input, downloadedFile);
      }
      assertThat(verifier.isValid(dump, downloadedFile)).isTrue();
    }
  }

  private List<DiscogsDump> getLatestCompleteDumpSet(List<DiscogsDump> dumps) throws Exception {
    DumpSupplier dumpSupplier = mock(DumpSupplier.class);
    when(dumpSupplier.get()).thenReturn(dumps);
    MapDiscogsDumpRepository repository = new MapDiscogsDumpRepository(dumpSupplier);
    repository.afterPropertiesSet();
    return new DefaultDiscogsDumpService(repository, dumpSupplier).getLatestCompleteDumpSet();
  }
}
