package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiscogsDumpUnitTest {

  private static final LocalDate DUMP_DATE = LocalDate.of(2026, 7, 1);

  @TempDir Path tempDirectory;

  @Test
  void shouldExposeEmptyInputWhenUrlIsAbsent() throws Exception {
    DiscogsDump dump = dump("etag", EntityType.ARTIST, null, 1L, DUMP_DATE, null);

    try (InputStream input = dump.getInputStream()) {
      assertThat(input.read()).isEqualTo(-1);
    }
  }

  @Test
  void shouldOpenConfiguredUrl() throws Exception {
    Path file = tempDirectory.resolve("artists.xml.gz");
    Files.writeString(file, "payload");
    DiscogsDump dump =
        dump("etag", EntityType.ARTIST, "data/2026/artists.xml.gz", 7L, DUMP_DATE,
            file.toUri().toURL());

    try (InputStream input = dump.getInputStream()) {
      assertThat(new String(input.readAllBytes())).isEqualTo("payload");
    }
    assertThat(dump.getFileName()).isEqualTo("artists.xml.gz");
  }

  @Test
  void shouldReturnNullFilenameForAbsentUri() {
    assertThat(dump("etag", EntityType.ARTIST, null, 1L, DUMP_DATE, null).getFileName()).isNull();
    assertThat(dump("etag", EntityType.ARTIST, " ", 1L, DUMP_DATE, null).getFileName()).isNull();
  }

  @Test
  void shouldCompareEveryStableOrderingField() {
    DiscogsDump baseline = dump("b", EntityType.ARTIST, "artist", 10L, DUMP_DATE, null);

    assertThat(baseline.compareTo(dump("a", EntityType.ARTIST, "artist", 10L,
        DUMP_DATE.plusDays(1), null))).isNegative();
    assertThat(baseline.compareTo(dump("a", EntityType.LABEL, "label", 10L, DUMP_DATE, null)))
        .isNegative();
    assertThat(baseline.compareTo(dump("c", EntityType.ARTIST, "artist", 10L, DUMP_DATE, null)))
        .isNegative();
    assertThat(baseline.compareTo(dump("b", EntityType.ARTIST, "artist", 11L, DUMP_DATE, null)))
        .isNegative();
  }

  @Test
  void equalityShouldUseStableDumpIdentifier() {
    DiscogsDump dump = dump("etag", EntityType.ARTIST, "artist", 1L, DUMP_DATE, null);
    DiscogsDump sameIdentifier = dump("etag", EntityType.RELEASE, "release", 2L, DUMP_DATE, null);
    DiscogsDump differentIdentifier = dump("other", EntityType.ARTIST, "artist", 1L, DUMP_DATE, null);

    assertThat(dump).isEqualTo(dump).isEqualTo(sameIdentifier).isNotEqualTo(differentIdentifier)
        .isNotEqualTo(null).isNotEqualTo("etag");
    assertThat(dump.hashCode()).isEqualTo(sameIdentifier.hashCode());
  }

  private static DiscogsDump dump(
      String eTag,
      EntityType type,
      String uri,
      Long size,
      LocalDate date,
      URL url) {
    return new DiscogsDump(eTag, type, uri, size, date, url);
  }
}
