package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.testutil.DiscogsDumpE2EFixture;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e")
class DefaultDumpSupplierE2ETest {

  @Test
  void whenGet__ThenReturnsNotEmptyListOfValidDiscogsDumps() {
    List<DiscogsDump> foundList = DiscogsDumpE2EFixture.getDumps();

    assertThat(foundList).isNotNull().isNotEmpty();
    assertThat(foundList)
        .extracting(DiscogsDump::getLastModifiedAt)
        .anyMatch(date -> date.getYear() == 2008)
        .anyMatch(date -> date.getYear() == LocalDate.now().getYear());
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
  void whenGetLatestDump__ThenItsChecksumManifestContainsTheFile() throws Exception {
    DiscogsDump latestDump =
        DiscogsDumpE2EFixture.getDumps().stream()
            .max(DiscogsDump::compareTo)
            .orElseThrow();

    assertThat(new DiscogsDumpVerifier().getChecksums(latestDump.getChecksumUrl()))
        .containsKey(latestDump.getFileName());
  }
}
