package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.repository.MapDiscogsDumpRepository;
import io.dsub.discogs.batch.dump.service.DefaultDiscogsDumpService;
import io.dsub.discogs.batch.testutil.DiscogsDumpE2EFixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e")
class DefaultDumpSupplierE2ETest {

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
                .satisfies(dump -> assertThat(dump.getSize()).isNotNull().isGreaterThan(0))
                .satisfies(dump -> assertThat(dump.getUriString()).isNotNull().isNotBlank())
                .satisfies(dump -> assertThat(dump.getFileName()).matches("^[\\w_]+\\.xml\\.gz$"))
                .satisfies(dump -> assertThat(dump.getChecksumUrl()).isNotNull())
                .satisfies(dump -> assertThat(dump.getType()).isNotNull()));
  }

  @Test
  void whenGetLatestCompleteDumpSet__ThenChecksumManifestContainsEveryFile() throws Exception {
    List<DiscogsDump> latestCompleteDumpSet =
        getLatestCompleteDumpSet(DiscogsDumpE2EFixture.getDumps());
    DiscogsDump firstDump = latestCompleteDumpSet.getFirst();

    assertThat(latestCompleteDumpSet)
        .extracting(DiscogsDump::getChecksumUrl)
        .containsOnly(firstDump.getChecksumUrl());
    Map<String, String> checksums =
        new DiscogsDumpVerifier().getChecksums(firstDump.getChecksumUrl());
    String[] fileNames =
        latestCompleteDumpSet.stream()
            .map(DiscogsDump::getFileName)
            .toArray(String[]::new);

    assertThat(checksums).containsKeys(fileNames);
  }

  private List<DiscogsDump> getLatestCompleteDumpSet(List<DiscogsDump> dumps) throws Exception {
    DumpSupplier dumpSupplier = mock(DumpSupplier.class);
    when(dumpSupplier.get()).thenReturn(dumps);
    MapDiscogsDumpRepository repository = new MapDiscogsDumpRepository(dumpSupplier);
    repository.afterPropertiesSet();
    return new DefaultDiscogsDumpService(repository, dumpSupplier).getLatestCompleteDumpSet();
  }
}
