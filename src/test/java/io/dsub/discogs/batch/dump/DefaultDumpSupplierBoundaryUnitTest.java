package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class DefaultDumpSupplierBoundaryUnitTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
    Thread.interrupted();
  }

  @Test
  void constructorAcceptsHttpAndHttpsAndRejectsEveryInvalidUriShape() {
    assertThat(new DefaultDumpSupplier(URI.create("http://example.test"))).isNotNull();
    assertThat(new DefaultDumpSupplier(URI.create("https://example.test/"))).isNotNull();
    assertThatThrownBy(() -> new DefaultDumpSupplier(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DefaultDumpSupplier(URI.create("relative")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DefaultDumpSupplier(URI.create("ftp://example.test")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void httpSourcesHandleSuccessNotFoundAndServerFailure() throws Exception {
    startServer();
    DefaultDumpSupplier supplier = new DefaultDumpSupplier(serverUri("/"));

    assertThat(supplier.getDiscogsDataSource(serverUri("/ok").toString())).isEqualTo("fixture");
    assertThatThrownBy(() -> supplier.getDiscogsDataSource(serverUri("/failure").toString()))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("HTTP 503");
    assertThat(supplier.getDiscogsManifestSource(serverUri("/missing").toString())).isEmpty();
    assertThat(supplier.getDiscogsManifestSource(serverUri("/ok").toString()))
        .contains("fixture");
    assertThatThrownBy(() -> supplier.getDiscogsManifestSource(serverUri("/failure").toString()))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("HTTP 503");
  }

  @Test
  void emptyIndexFallbackHandlesInterruptAndIoFailureWithoutRetryStorm() throws Exception {
    DefaultDumpSupplier interrupted = spy(new DefaultDumpSupplier());
    doReturn("").when(interrupted).getDiscogsDataSource("https://data.discogs.com/");
    doThrow(new InterruptedException("fixture"))
        .when(interrupted)
        .getLatestCompleteDumpsFromManifests();

    assertThat(interrupted.get()).isEmpty();
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    Thread.interrupted();

    DefaultDumpSupplier unavailable = spy(new DefaultDumpSupplier());
    doReturn("").when(unavailable).getDiscogsDataSource("https://data.discogs.com/");
    doThrow(new IOException("fixture"))
        .when(unavailable)
        .getLatestCompleteDumpsFromManifests();
    assertThat(unavailable.get()).isEmpty();
  }

  @Test
  void manifestLookbackStopsAfterTheBoundedWindow() throws Exception {
    DefaultDumpSupplier supplier = spy(new DefaultDumpSupplier());
    doReturn(LocalDate.of(2026, 8, 10)).when(supplier).getCurrentUtcDate();
    doReturn(Optional.empty()).when(supplier).getDiscogsManifestSource(org.mockito.ArgumentMatchers.anyString());

    assertThat(supplier.getLatestCompleteDumpsFromManifests()).isEmpty();
    assertThat(new DefaultDumpSupplier().getCurrentUtcDate()).isEqualTo(LocalDate.now(java.time.ZoneOffset.UTC));
  }

  @Test
  void checksumAndDumpHtmlParsersSkipMismatchedAndMalformedEntries() {
    DefaultDumpSupplier supplier = new DefaultDumpSupplier();
    String mismatchedChecksum =
        "<a href=\"?download=data%2F2026%2Fdiscogs_20260601_CHECKSUM.txt\">"
            + "discogs_20260701_CHECKSUM.txt</a>";
    String invalidDateChecksum =
        "<a href=\"?download=data%2F2026%2Fdiscogs_20261301_CHECKSUM.txt\">"
            + "discogs_20261301_CHECKSUM.txt</a>";
    assertThat(supplier.parseChecksumUrls(mismatchedChecksum)).isEmpty();
    assertThat(supplier.parseChecksumUrls(invalidDateChecksum)).isEmpty();

    String badPath =
        "2026-07-01 00:00:00 1 MB "
            + "<a href=\"?download=discogs_20260701_artists.xml.gz\">"
            + "discogs_20260701_artists.xml.gz</a>";
    assertThat(supplier.parseHtmlDumpList(badPath)).isEmpty();

    String invalidPatternWithMatchingFilename =
        "2026-07-01 00:00:00 1 MB "
            + "<a href=\"?download=data%2F2026.bad%2Fdiscogs_20260701_artists.xml.gz\">"
            + "discogs_20260701_artists.xml.gz</a>";
    assertThat(supplier.parseHtmlDumpList(invalidPatternWithMatchingFilename)).isEmpty();
  }

  @Test
  void legacyCatalogBoundariesReturnEmptyInsteadOfNull(@TempDir Path tempDir) throws Exception {
    DefaultDumpSupplier supplier = new DefaultDumpSupplier();
    assertThat(supplier.parseDumpList(null)).isEmpty();
    Path malformed = Files.writeString(tempDir.resolve("malformed.xml"), "not XML");
    assertThat(supplier.parseDumpList(malformed.toFile())).isEmpty();
    assertThat(supplier.get(malformed.toFile())).isEmpty();

    NodeList noNodes = mock(NodeList.class);
    when(noNodes.getLength()).thenReturn(0);
    assertThat(supplier.parseDump(noNodes)).isNull();

    Node key = node("Key", "");
    Node etag = node("ETag", "etag");
    Node size = node("Size", "1");
    NodeList blankKnownNode = nodeList(key, etag, size);
    assertThat(supplier.parseDump(blankKnownNode)).isNull();

    Node invalidKey = node("Key", "data/2026/discogs_20260701_unknown.xml.gz");
    assertThat(supplier.parseDump(nodeList(invalidKey, etag, size))).isNull();

    Node nullText = node("Key", null);
    assertThat(supplier.parseDump(nodeList(nullText, etag, size))).isNull();
    assertThat(supplier.isXmlGzipEntry(nodeList(node("Other", "plain.txt")))).isFalse();
    assertThat(supplier.isXmlGzipEntry(nodeList(node("Other", "data/a.xml.gz")))).isTrue();
  }

  @Test
  void dateTypeAndTimestampHelpersRejectMalformedBoundaries() {
    DefaultDumpSupplier supplier = new DefaultDumpSupplier();
    assertThatThrownBy(() -> supplier.parseLastModifiedAt("no-date"))
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessageContaining("does not contain a date");
    Node artist = node("Key", "data/2026/discogs_20260701_artists.xml.gz");
    assertThat(supplier.getType(artist)).isEqualTo(EntityType.ARTIST);
    assertThatThrownBy(() -> supplier.getUTCLastModified(node("LastModified", null)))
        .isInstanceOf(InvalidArgumentException.class);
  }

  private void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/ok", exchange -> respond(exchange, 200, "fixture"));
    server.createContext("/missing", exchange -> respond(exchange, 404, "missing"));
    server.createContext("/failure", exchange -> respond(exchange, 503, "failure"));
    server.start();
  }

  private URI serverUri(String path) {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
  }

  private void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(status, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private Node node(String name, String text) {
    Node node = mock(Node.class);
    when(node.getNodeName()).thenReturn(name);
    when(node.getTextContent()).thenReturn(text);
    return node;
  }

  private NodeList nodeList(Node... nodes) {
    NodeList nodeList = mock(NodeList.class);
    when(nodeList.getLength()).thenReturn(nodes.length);
    for (int index = 0; index < nodes.length; index++) {
      when(nodeList.item(index)).thenReturn(nodes[index]);
    }
    return nodeList;
  }
}
