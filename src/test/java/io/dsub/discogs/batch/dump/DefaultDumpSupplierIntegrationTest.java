package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultDumpSupplierIntegrationTest {

  private static final String DATE_STAMP = "20260701";
  private static final String MANIFEST_NAME = "discogs_" + DATE_STAMP + "_CHECKSUM.txt";

  private final List<String> requests = new ArrayList<>();
  private HttpServer server;
  private Map<String, byte[]> dumpPayloads;
  private byte[] manifestPayload;

  @BeforeEach
  void startServer() throws Exception {
    dumpPayloads = createDumpPayloads();
    manifestPayload = createManifest(dumpPayloads).getBytes(StandardCharsets.UTF_8);
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext("/", this::handleRequest);
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void selectedCatalogPinsEveryDumpBeforeReturning() {
    List<DiscogsDump> dumps = supplier().get();

    assertThat(dumps)
        .hasSize(EntityType.values().length)
        .extracting(DiscogsDump::getType)
        .containsExactly(EntityType.values());
    assertThat(dumps)
        .allSatisfy(
            dump -> {
              assertThat(dump.getETag()).isNotBlank();
              assertThat(dump.getSize()).isEqualTo(dumpPayloads.get(dump.getFileName()).length);
              assertThat(dump.getUriString()).isNotBlank();
              assertThat(dump.getFileName()).matches("^[\\w_]+\\.xml\\.gz$");
              assertThat(dump.getChecksumUrl()).isNotNull();
              assertThat(dump.getChecksumSha256()).hasSize(64);
            });
    assertThat(requests)
        .containsExactly(
            "GET <root>",
            "GET prefix=data%2F2026%2F",
            "GET download=data%2F2026%2F" + MANIFEST_NAME,
            "HEAD " + dumpQuery(EntityType.ARTIST),
            "HEAD " + dumpQuery(EntityType.LABEL),
            "HEAD " + dumpQuery(EntityType.MASTER),
            "HEAD " + dumpQuery(EntityType.RELEASE));
  }

  @Test
  void pinnedDownloadsVerifyWithoutFetchingTheManifestAgain(@TempDir Path tempDir)
      throws Exception {
    List<DiscogsDump> dumps = supplier().get();
    int requestsBeforeDownload = requests.size();
    DiscogsDumpVerifier verifier = new DiscogsDumpVerifier();

    for (DiscogsDump dump : dumps) {
      Path downloadedFile = tempDir.resolve(dump.getFileName());
      try (var input = dump.getInputStream()) {
        Files.copy(input, downloadedFile);
      }
      assertThat(verifier.isValid(dump, downloadedFile)).isTrue();
    }

    assertThat(requests.subList(requestsBeforeDownload, requests.size()))
        .containsExactly(
            "GET " + dumpQuery(EntityType.ARTIST),
            "GET " + dumpQuery(EntityType.LABEL),
            "GET " + dumpQuery(EntityType.MASTER),
            "GET " + dumpQuery(EntityType.RELEASE));
  }

  private DefaultDumpSupplier supplier() {
    return new DefaultDumpSupplier(serverUri());
  }

  private URI serverUri() {
    return URI.create(
        "http://"
            + server.getAddress().getHostString()
            + ":"
            + server.getAddress().getPort()
            + "/");
  }

  private void handleRequest(HttpExchange exchange) throws IOException {
    String rawQuery = exchange.getRequestURI().getRawQuery();
    requests.add(exchange.getRequestMethod() + " " + (rawQuery == null ? "<root>" : rawQuery));
    if (rawQuery == null) {
      respond(exchange, 200, "text/html", rootIndex().getBytes(StandardCharsets.UTF_8));
      return;
    }
    if (rawQuery.startsWith("prefix=")) {
      respond(exchange, 200, "text/html", yearIndex().getBytes(StandardCharsets.UTF_8));
      return;
    }
    String fileName = requestedFileName(rawQuery);
    if (MANIFEST_NAME.equals(fileName)) {
      respond(exchange, 200, "text/plain", manifestPayload);
      return;
    }
    byte[] payload = dumpPayloads.get(fileName);
    if (payload == null) {
      respond(exchange, 404, "text/plain", new byte[0]);
      return;
    }
    if ("HEAD".equals(exchange.getRequestMethod())) {
      exchange.getResponseHeaders().set("Content-Length", Integer.toString(payload.length));
      exchange.sendResponseHeaders(200, -1);
      exchange.close();
      return;
    }
    respond(exchange, 200, "application/gzip", payload);
  }

  private void respond(HttpExchange exchange, int status, String contentType, byte[] payload)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(status, payload.length);
    try (var responseBody = exchange.getResponseBody()) {
      responseBody.write(payload);
    }
  }

  private String requestedFileName(String rawQuery) {
    String path =
        URLDecoder.decode(rawQuery.substring("download=".length()), StandardCharsets.UTF_8);
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private String rootIndex() {
    return "<a href=\"?prefix=data%2F2026%2F\">2026/</a>";
  }

  private String yearIndex() {
    StringBuilder html =
        new StringBuilder(
            "<a href=\"?download=data%2F2026%2F"
                + MANIFEST_NAME
                + "\">"
                + MANIFEST_NAME
                + "</a>\n");
    for (EntityType type : EntityType.values()) {
      String fileName = dumpFileName(type);
      html.append("2026-07-01 00:00:00 1 MB <a href=\"?")
          .append(dumpQuery(type))
          .append("\">")
          .append(fileName)
          .append("</a>\n");
    }
    return html.toString();
  }

  private Map<String, byte[]> createDumpPayloads() throws IOException {
    Map<String, byte[]> values = new LinkedHashMap<>();
    for (EntityType type : EntityType.values()) {
      values.put(
          dumpFileName(type),
          gzip("<" + type + "s><" + type + " id=\"1\"/></" + type + "s>"));
    }
    return Map.copyOf(values);
  }

  private String createManifest(Map<String, byte[]> payloads) {
    StringBuilder manifest = new StringBuilder();
    payloads.forEach(
        (fileName, payload) ->
            manifest.append(sha256(payload)).append("  ").append(fileName).append('\n'));
    return manifest.toString();
  }

  private byte[] gzip(String content) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
      gzip.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return output.toByteArray();
  }

  private String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private String dumpFileName(EntityType type) {
    return "discogs_" + DATE_STAMP + "_" + type + "s.xml.gz";
  }

  private String dumpQuery(EntityType type) {
    return "download=data%2F2026%2F" + dumpFileName(type);
  }
}
