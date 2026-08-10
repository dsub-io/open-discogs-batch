package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.exception.FileException;
import java.net.URI;
import java.net.URL;
import java.net.ServerSocket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiscogsDumpVerifierUnitTest {

  private static final String CONTENT = "open-discogs";
  private static final String SHA_256 =
      "c769e39c5ee3334938803e70d256fc9300e6d4edb303c508165313807e6b9053";

  @TempDir Path tempDir;

  @Test
  void whenChecksumMatches__ThenFileIsValidAndManifestIsCached() throws Exception {
    Path file = Files.writeString(tempDir.resolve("discogs_20260701_releases.xml.gz"), CONTENT);
    URL checksumUrl = URI.create("https://example.test/discogs_20260701_CHECKSUM.txt").toURL();
    URI checksumUri = checksumUrl.toURI();
    DiscogsDump dump = dump(file, checksumUrl);
    DiscogsDumpVerifier verifier = spy(new DiscogsDumpVerifier());
    doReturn(SHA_256 + "  " + file.getFileName())
        .when(verifier)
        .getChecksumSource(checksumUri);

    assertThat(verifier.isValid(dump, file)).isTrue();
    assertThat(verifier.isValid(dump, file)).isTrue();
    verify(verifier, times(1)).getChecksumSource(checksumUri);
  }

  @Test
  void whenChecksumDoesNotMatch__ThenFileIsInvalid() throws Exception {
    Path file = Files.writeString(tempDir.resolve("discogs_20260701_releases.xml.gz"), CONTENT);
    URL checksumUrl = URI.create("https://example.test/discogs_20260701_CHECKSUM.txt").toURL();
    DiscogsDump dump = dump(file, checksumUrl);
    DiscogsDumpVerifier verifier = spy(new DiscogsDumpVerifier());
    doReturn("0".repeat(64) + " *" + file.getFileName())
        .when(verifier)
        .getChecksumSource(checksumUrl.toURI());

    assertThat(verifier.isValid(dump, file)).isFalse();
  }

  @Test
  void whenManifestUsesPaths__ThenParserKeepsOnlyTheFileName() {
    assertThat(
            DiscogsDumpVerifier.parseChecksums(
                SHA_256
                    + "  data/2026/discogs_20260701_releases.xml.gz\n"
                    + "invalid line\n"))
        .isEqualTo(Map.of("discogs_20260701_releases.xml.gz", SHA_256));
    assertThat(DiscogsDumpVerifier.parseChecksums("")).isEmpty();
    assertThat(DiscogsDumpVerifier.parseChecksums(null)).isEmpty();
  }

  @Test
  void whenManifestDoesNotContainDump__ThenVerificationFails() throws Exception {
    Path file = Files.writeString(tempDir.resolve("discogs_20260701_releases.xml.gz"), CONTENT);
    URL checksumUrl = URI.create("https://example.test/discogs_20260701_CHECKSUM.txt").toURL();
    DiscogsDumpVerifier verifier = spy(new DiscogsDumpVerifier());
    doReturn(SHA_256 + "  discogs_20260701_artists.xml.gz")
        .when(verifier)
        .getChecksumSource(checksumUrl.toURI());

    assertThatThrownBy(() -> verifier.isValid(dump(file, checksumUrl), file))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("checksum for discogs_20260701_releases.xml.gz is missing");
  }

  @Test
  void whenManifestHasNoValidEntries__ThenVerificationFails() throws Exception {
    Path file = Files.writeString(tempDir.resolve("discogs_20260701_releases.xml.gz"), CONTENT);
    URL checksumUrl = URI.create("https://example.test/discogs_20260701_CHECKSUM.txt").toURL();
    DiscogsDumpVerifier verifier = spy(new DiscogsDumpVerifier());
    doReturn("not a checksum").when(verifier).getChecksumSource(checksumUrl.toURI());

    assertThatThrownBy(() -> verifier.isValid(dump(file, checksumUrl), file))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("no SHA-256 entries found");
  }

  @Test
  void expectedChecksumUsesTheSameCachedManifestAsFileVerification() throws Exception {
    Path file = Files.writeString(tempDir.resolve("discogs_20260701_releases.xml.gz"), CONTENT);
    URL checksumUrl = URI.create("https://example.test/discogs_20260701_CHECKSUM.txt").toURL();
    DiscogsDump dump = dump(file, checksumUrl);
    DiscogsDumpVerifier verifier = spy(new DiscogsDumpVerifier());
    doReturn(SHA_256 + "  " + file.getFileName())
        .when(verifier)
        .getChecksumSource(checksumUrl.toURI());

    assertThat(verifier.getExpectedChecksum(dump)).isEqualTo(SHA_256);
    assertThat(verifier.isValid(dump, file)).isTrue();
    verify(verifier, times(1)).getChecksumSource(checksumUrl.toURI());
  }

  @Test
  void whenLegacyDumpHasNoChecksum__ThenExactSizeIsUsed() throws Exception {
    Path file = Files.writeString(tempDir.resolve("legacy.xml.gz"), CONTENT);
    DiscogsDump dump =
        new DiscogsDump(
            "etag",
            EntityType.RELEASE,
            "data/2020/legacy.xml.gz",
            Files.size(file),
            LocalDate.of(2020, 1, 1),
            null);
    DiscogsDumpVerifier verifier = new DiscogsDumpVerifier();

    assertThat(verifier.isValid(dump, file)).isTrue();
  }

  @Test
  void expectedChecksumRequiresAManifestAndTheRequestedEntry() throws Exception {
    Path file = Files.writeString(tempDir.resolve("legacy.xml.gz"), CONTENT);
    DiscogsDump legacy =
        new DiscogsDump(
            "etag",
            EntityType.RELEASE,
            "data/2020/legacy.xml.gz",
            Files.size(file),
            LocalDate.of(2020, 1, 1),
            null);
    assertThatThrownBy(() -> new DiscogsDumpVerifier().getExpectedChecksum(legacy))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("manifest is required");

    URL checksumUrl = URI.create("https://example.test/checksum.txt").toURL();
    DiscogsDumpVerifier verifier = spy(new DiscogsDumpVerifier());
    doReturn(SHA_256 + "  another.xml.gz")
        .when(verifier)
        .getChecksumSource(checksumUrl.toURI());
    assertThatThrownBy(() -> verifier.getExpectedChecksum(dump(file, checksumUrl)))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("checksum for legacy.xml.gz is missing");
  }

  @Test
  void invalidManifestUrlAndUnreadableFileReturnFileErrors() throws Exception {
    URL invalid = mock(URL.class);
    when(invalid.toURI()).thenThrow(new URISyntaxException("fixture", "invalid"));
    assertThatThrownBy(() -> new DiscogsDumpVerifier().getChecksums(invalid))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("invalid checksum URL");

    assertThatThrownBy(
            () -> new DiscogsDumpVerifier().calculateSha256(tempDir.resolve("missing.xml.gz")))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("failed to calculate SHA-256");
  }

  @Test
  void checksumTransportHandlesHttpFailureIoFailureAndInterrupt() throws Exception {
    DiscogsDumpVerifier verifier = new DiscogsDumpVerifier();
    com.sun.net.httpserver.HttpServer server =
        com.sun.net.httpserver.HttpServer.create(
            new java.net.InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/failure",
        exchange -> {
          exchange.sendResponseHeaders(503, -1);
          exchange.close();
        });
    server.start();
    try {
      URI failure =
          URI.create(
              "http://127.0.0.1:" + server.getAddress().getPort() + "/failure");
      assertThatThrownBy(() -> verifier.getChecksumSource(failure))
          .isInstanceOf(FileException.class)
          .hasMessageContaining("HTTP 503");
    } finally {
      server.stop(0);
    }

    int unusedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      unusedPort = socket.getLocalPort();
    }
    URI unavailable = URI.create("http://127.0.0.1:" + unusedPort + "/checksum");
    assertThatThrownBy(() -> verifier.getChecksumSource(unavailable))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("failed to fetch checksum");

    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(() -> verifier.getChecksumSource(unavailable))
          .isInstanceOf(FileException.class)
          .hasMessageContaining("interrupted while fetching checksum");
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void legacySizeVerificationHandlesNullMismatchAndUnreadableFile() throws Exception {
    Path file = Files.writeString(tempDir.resolve("legacy.xml.gz"), CONTENT);
    DiscogsDumpVerifier verifier = new DiscogsDumpVerifier();
    DiscogsDump nullSize =
        new DiscogsDump(
            "etag", EntityType.RELEASE, "legacy.xml.gz", null, LocalDate.of(2020, 1, 1), null);
    DiscogsDump wrongSize =
        new DiscogsDump(
            "etag", EntityType.RELEASE, "legacy.xml.gz", 1L, LocalDate.of(2020, 1, 1), null);
    assertThat(verifier.isValid(nullSize, file)).isFalse();
    assertThat(verifier.isValid(wrongSize, file)).isFalse();
    assertThatThrownBy(() -> verifier.isValid(wrongSize, tempDir.resolve("missing.xml.gz")))
        .isInstanceOf(FileException.class)
        .hasMessageContaining("failed to fetch size");
  }

  private DiscogsDump dump(Path file, URL checksumUrl) throws Exception {
    return new DiscogsDump(
        file.toString(),
        EntityType.RELEASE,
        "data/2026/" + file.getFileName(),
        Files.size(file),
        LocalDate.of(2026, 7, 1),
        file.toUri().toURL(),
        checksumUrl);
  }
}
