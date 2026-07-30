package io.dsub.discogs.batch.testutil;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.dsub.discogs.batch.dump.DefaultDumpSupplier;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

public final class DiscogsDumpE2EFixture {

  private static final String DATE_STAMP = "20260701";
  private static final String ROOT_INDEX =
      """
      <!DOCTYPE html>
      <html>
      <body>
      <pre>
      Last Modified                   Size           Name
      --------------------------------------------------
                                      -              <a href="?prefix=data%2F2026%2F">2026/</a>
      </pre>
      </body>
      </html>
      """;

  private static HttpServer server;
  private static List<DiscogsDump> dumps;
  private static Map<String, byte[]> dumpPayloads;
  private static byte[] manifestPayload;
  private static byte[] yearIndexPayload;

  private DiscogsDumpE2EFixture() {
  }

  public static synchronized List<DiscogsDump> getDumps() {
    if (dumps == null) {
      try {
        startServer();
      } catch (IOException e) {
        throw new IllegalStateException("failed to start the Discogs E2E fixture server", e);
      }
      List<DiscogsDump> fetchedDumps = new FixtureDumpSupplier(getServerUri()).get();
      if (fetchedDumps.isEmpty()) {
        shutdown();
        throw new IllegalStateException("Discogs E2E fixture returned no usable dumps");
      }
      dumps = List.copyOf(fetchedDumps);
    }
    return dumps;
  }

  public static synchronized void shutdown() {
    if (server != null) {
      server.stop(0);
    }
    server = null;
    dumps = null;
    dumpPayloads = null;
    manifestPayload = null;
    yearIndexPayload = null;
  }

  private static void startServer() throws IOException {
    if (server != null) {
      return;
    }
    dumpPayloads = createDumpPayloads();
    manifestPayload = createManifest(dumpPayloads).getBytes(StandardCharsets.UTF_8);
    yearIndexPayload = readResource("DiscogsData2026.html");
    server =
        HttpServer.create(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
            0);
    server.createContext("/", DiscogsDumpE2EFixture::handleRequest);
    server.setExecutor(
        Executors.newCachedThreadPool(
            runnable -> {
              Thread thread = new Thread(runnable, "discogs-e2e-fixture");
              thread.setDaemon(true);
              return thread;
            }));
    server.start();
  }

  private static void handleRequest(HttpExchange exchange) throws IOException {
    if (!"GET".equals(exchange.getRequestMethod())) {
      respond(exchange, 405, "text/plain", new byte[0]);
      return;
    }

    String rawQuery = exchange.getRequestURI().getRawQuery();
    if (rawQuery == null) {
      respond(
          exchange,
          200,
          "text/html; charset=utf-8",
          ROOT_INDEX.getBytes(StandardCharsets.UTF_8));
      return;
    }
    if (rawQuery.startsWith("prefix=")) {
      respond(exchange, 200, "text/html; charset=utf-8", yearIndexPayload);
      return;
    }
    if (!rawQuery.startsWith("download=")) {
      respond(exchange, 404, "text/plain", new byte[0]);
      return;
    }

    String path =
        URLDecoder.decode(
            rawQuery.substring("download=".length()),
            StandardCharsets.UTF_8);
    String fileName = path.substring(path.lastIndexOf('/') + 1);
    if (fileName.endsWith("_CHECKSUM.txt")) {
      respond(exchange, 200, "text/plain; charset=utf-8", manifestPayload);
      return;
    }
    byte[] payload = dumpPayloads.get(fileName);
    if (payload == null) {
      respond(exchange, 404, "text/plain", new byte[0]);
      return;
    }
    respond(exchange, 200, "application/gzip", payload);
  }

  private static void respond(
      HttpExchange exchange, int status, String contentType, byte[] payload)
      throws IOException {
    exchange.getResponseHeaders().set("Content-Type", contentType);
    exchange.sendResponseHeaders(status, payload.length);
    try (var responseBody = exchange.getResponseBody()) {
      responseBody.write(payload);
    }
  }

  private static Map<String, byte[]> createDumpPayloads() throws IOException {
    Map<String, byte[]> payloads = new LinkedHashMap<>();
    for (EntityType type : EntityType.values()) {
      String fileName = "discogs_" + DATE_STAMP + "_" + type + "s.xml.gz";
      payloads.put(
          fileName,
          gzip("<" + type + "s><" + type + " id=\"1\"/></" + type + "s>"));
    }
    return Map.copyOf(payloads);
  }

  private static String createManifest(Map<String, byte[]> payloads) {
    StringBuilder manifest = new StringBuilder();
    payloads.forEach(
        (fileName, payload) ->
            manifest
                .append(sha256(payload))
                .append("  ")
                .append(fileName)
                .append('\n'));
    return manifest.toString();
  }

  private static byte[] gzip(String content) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
      gzip.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return output.toByteArray();
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  private static byte[] readResource(String name) throws IOException {
    try (var input =
        DiscogsDumpE2EFixture.class.getResourceAsStream("/test/" + name)) {
      if (input == null) {
        throw new IOException("missing E2E fixture resource: " + name);
      }
      return input.readAllBytes();
    }
  }

  private static URI getServerUri() {
    return URI.create(
        "http://"
            + server.getAddress().getHostString()
            + ":"
            + server.getAddress().getPort()
            + "/");
  }

  private static final class FixtureDumpSupplier extends DefaultDumpSupplier {

    private FixtureDumpSupplier(URI baseUri) {
      super(baseUri);
    }
  }
}
