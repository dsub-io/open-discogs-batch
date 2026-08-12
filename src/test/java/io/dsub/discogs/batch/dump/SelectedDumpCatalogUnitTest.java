package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SelectedDumpCatalogUnitTest {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
  private static final String ARTIST_CHECKSUM = "a".repeat(64);
  private static final String RELEASE_CHECKSUM = "b".repeat(64);
  private static final String REISSUED_ARTIST_CHECKSUM = "c".repeat(64);
  private static final long ARTIST_SIZE = 494_557_848L;
  private static final long RELEASE_SIZE = 11_234_567_890L;
  private static final String ROOT_REQUEST = "<root>";
  private static final String GET_METHOD = "GET";
  private static final String HEAD_METHOD = "HEAD";

  private final List<String> requests = new ArrayList<>();
  private HttpServer server;

  @AfterEach
  void stopServerAndClearInterrupt() {
    if (server != null) {
      server.stop(0);
    }
    Thread.interrupted();
  }

  @Test
  void exactMonthPinsListingUrlsChecksumsAndExactSizesOnce() throws Exception {
    LocalDate firstDate = LocalDate.of(2026, 7, 1);
    LocalDate reissuedDate = LocalDate.of(2026, 7, 15);
    startServer(
        Map.of(
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(
                        entry(firstDate, EntityType.ARTIST),
                        entry(reissuedDate, EntityType.ARTIST),
                        entry(firstDate, EntityType.ARTIST),
                        entry(firstDate, EntityType.RELEASE)),
                    List.of(firstDate, reissuedDate))),
            get(downloadRequest(reissuedDate)),
            response(
                200,
                manifestLine(
                    reissuedDate, EntityType.ARTIST, REISSUED_ARTIST_CHECKSUM)),
            get(downloadRequest(firstDate)),
            response(
                200,
                manifestLine(firstDate, EntityType.ARTIST, ARTIST_CHECKSUM)
                    + manifestLine(firstDate, EntityType.RELEASE, RELEASE_CHECKSUM)),
            head(dumpRequest(reissuedDate, EntityType.ARTIST)),
            headResponse(200, ARTIST_SIZE),
            head(dumpRequest(firstDate, EntityType.RELEASE)),
            headResponse(200, RELEASE_SIZE)));

    List<DiscogsDump> dumps =
        supplier().getMonth(
            EnumSet.of(EntityType.ARTIST, EntityType.RELEASE), YearMonth.from(firstDate));

    assertThat(requests)
        .containsExactly(
            get(prefixRequest(2026)),
            get(downloadRequest(reissuedDate)),
            get(downloadRequest(firstDate)),
            head(dumpRequest(reissuedDate, EntityType.ARTIST)),
            head(dumpRequest(firstDate, EntityType.RELEASE)));
    assertThat(dumps)
        .extracting(DiscogsDump::getType)
        .containsExactly(EntityType.ARTIST, EntityType.RELEASE);
    assertThat(dumps)
        .extracting(DiscogsDump::getLastModifiedAt)
        .containsExactly(reissuedDate, firstDate);
    assertThat(dumps)
        .extracting(DiscogsDump::getChecksumSha256)
        .containsExactly(REISSUED_ARTIST_CHECKSUM, RELEASE_CHECKSUM);
    assertThat(dumps)
        .extracting(DiscogsDump::getSize)
        .containsExactly(ARTIST_SIZE, RELEASE_SIZE);
    assertThat(dumps)
        .allSatisfy(
            dump ->
                assertThat(dump.getUrl().toString())
                    .isEqualTo(serverUri() + "?" + dumpRequest(dump.getLastModifiedAt(), dump.getType())));
  }

  @Test
  void latestUsesRootNewestYearAndOneManifestPerDistinctSelectedDate() throws Exception {
    LocalDate artistDate = LocalDate.of(2026, 7, 1);
    LocalDate releaseDate = LocalDate.of(2026, 6, 1);
    startServer(
        Map.of(
            get(ROOT_REQUEST),
            response(200, rootIndex(2025, 2026, 2025)),
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(
                        entry(artistDate, EntityType.ARTIST),
                        entry(releaseDate, EntityType.RELEASE)),
                    List.of(artistDate, releaseDate))),
            get(downloadRequest(artistDate)),
            response(200, manifestLine(artistDate, EntityType.ARTIST, ARTIST_CHECKSUM)),
            get(downloadRequest(releaseDate)),
            response(200, manifestLine(releaseDate, EntityType.RELEASE, RELEASE_CHECKSUM)),
            head(dumpRequest(artistDate, EntityType.ARTIST)),
            headResponse(200, ARTIST_SIZE),
            head(dumpRequest(releaseDate, EntityType.RELEASE)),
            headResponse(200, RELEASE_SIZE)));

    List<DiscogsDump> dumps =
        supplier().getLatest(EnumSet.of(EntityType.ARTIST, EntityType.RELEASE));

    assertThat(requests)
        .containsExactly(
            get(ROOT_REQUEST),
            get(prefixRequest(2026)),
            get(downloadRequest(artistDate)),
            get(downloadRequest(releaseDate)),
            head(dumpRequest(artistDate, EntityType.ARTIST)),
            head(dumpRequest(releaseDate, EntityType.RELEASE)));
    assertThat(dumps)
        .extracting(DiscogsDump::getLastModifiedAt)
        .containsExactly(artistDate, releaseDate);
  }

  @Test
  void latestSharesOneManifestWhenSelectedEntitiesHaveTheSameDate() throws Exception {
    LocalDate date = LocalDate.of(2026, 7, 1);
    startServer(
        Map.of(
            get(ROOT_REQUEST),
            response(200, rootIndex(2026)),
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(
                        entry(date, EntityType.ARTIST),
                        entry(date, EntityType.RELEASE)),
                    List.of(date))),
            get(downloadRequest(date)),
            response(
                200,
                manifestLine(date, EntityType.ARTIST, ARTIST_CHECKSUM)
                    + manifestLine(date, EntityType.RELEASE, RELEASE_CHECKSUM)),
            head(dumpRequest(date, EntityType.ARTIST)),
            headResponse(200, ARTIST_SIZE),
            head(dumpRequest(date, EntityType.RELEASE)),
            headResponse(200, RELEASE_SIZE)));

    supplier().getLatest(EnumSet.of(EntityType.ARTIST, EntityType.RELEASE));

    assertThat(requests)
        .containsExactly(
            get(ROOT_REQUEST),
            get(prefixRequest(2026)),
            get(downloadRequest(date)),
            head(dumpRequest(date, EntityType.ARTIST)),
            head(dumpRequest(date, EntityType.RELEASE)));
  }

  @Test
  void latestReadsEachOlderYearOnceOnlyUntilEveryEntityIsFound() throws Exception {
    LocalDate artistDate = LocalDate.of(2026, 1, 1);
    LocalDate releaseDate = LocalDate.of(2025, 12, 1);
    Map<String, StubResponse> responses = new LinkedHashMap<>();
    responses.put(get(ROOT_REQUEST), response(200, rootIndex(2025, 2026)));
    responses.put(
        get(prefixRequest(2026)),
        response(
            200,
            yearIndex(
                List.of(entry(artistDate, EntityType.ARTIST)), List.of(artistDate))));
    responses.put(
        get(prefixRequest(2025)),
        response(
            200,
            yearIndex(
                List.of(entry(releaseDate, EntityType.RELEASE)), List.of(releaseDate))));
    responses.put(
        get(downloadRequest(artistDate)),
        response(200, manifestLine(artistDate, EntityType.ARTIST, ARTIST_CHECKSUM)));
    responses.put(
        get(downloadRequest(releaseDate)),
        response(200, manifestLine(releaseDate, EntityType.RELEASE, RELEASE_CHECKSUM)));
    responses.put(
        head(dumpRequest(artistDate, EntityType.ARTIST)), headResponse(200, ARTIST_SIZE));
    responses.put(
        head(dumpRequest(releaseDate, EntityType.RELEASE)), headResponse(200, RELEASE_SIZE));
    startServer(responses);

    List<DiscogsDump> dumps =
        supplier().getLatest(EnumSet.of(EntityType.ARTIST, EntityType.RELEASE));

    assertThat(requests)
        .containsExactly(
            get(ROOT_REQUEST),
            get(prefixRequest(2026)),
            get(prefixRequest(2025)),
            get(downloadRequest(artistDate)),
            get(downloadRequest(releaseDate)),
            head(dumpRequest(artistDate, EntityType.ARTIST)),
            head(dumpRequest(releaseDate, EntityType.RELEASE)));
    assertThat(dumps)
        .extracting(DiscogsDump::getLastModifiedAt)
        .containsExactly(artistDate, releaseDate);
  }

  @Test
  void missingSelectionDoesNotFetchManifestOrObjectMetadata() throws Exception {
    LocalDate date = LocalDate.of(2026, 7, 1);
    startServer(
        Map.of(
            get(ROOT_REQUEST),
            response(200, rootIndex(2026)),
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(entry(date, EntityType.ARTIST)), List.of(date)))));

    assertThatThrownBy(
            () ->
                supplier().getLatest(
                    EnumSet.of(EntityType.ARTIST, EntityType.RELEASE)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("release");
    assertThat(requests).containsExactly(get(ROOT_REQUEST), get(prefixRequest(2026)));
  }

  @Test
  void incompleteManifestIsRejectedBeforeAnyObjectMetadataRequest() throws Exception {
    LocalDate date = LocalDate.of(2026, 7, 1);
    startServer(
        Map.of(
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(
                        entry(date, EntityType.ARTIST),
                        entry(date, EntityType.RELEASE)),
                    List.of(date))),
            get(downloadRequest(date)),
            response(200, manifestLine(date, EntityType.ARTIST, ARTIST_CHECKSUM))));

    assertThatThrownBy(
            () ->
                supplier().getMonth(
                    EnumSet.of(EntityType.ARTIST, EntityType.RELEASE), YearMonth.from(date)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("releases.xml.gz");
    assertThat(requests)
        .containsExactly(get(prefixRequest(2026)), get(downloadRequest(date)));
  }

  @Test
  void missingManifestStopsAtTheSelectedDate() throws Exception {
    LocalDate date = LocalDate.of(2026, 7, 1);
    startServer(
        Map.of(
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(entry(date, EntityType.ARTIST)), List.of(date))),
            get(downloadRequest(date)),
            response(404, "missing")));

    assertThatThrownBy(
            () -> supplier().getMonth(EnumSet.of(EntityType.ARTIST), YearMonth.from(date)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("manifest not found");
    assertThat(requests)
        .containsExactly(get(prefixRequest(2026)), get(downloadRequest(date)));
  }

  @Test
  void accessRejectionStopsWithoutFallbackRequests() throws Exception {
    LocalDate date = LocalDate.of(2026, 7, 1);
    startServer(Map.of(get(prefixRequest(2026)), response(429, "rate limited")));

    assertThatThrownBy(
            () -> supplier().getMonth(EnumSet.of(EntityType.ARTIST), YearMonth.from(date)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("HTTP 429");
    assertThat(requests).containsExactly(get(prefixRequest(2026)));
  }

  @Test
  void latestYearAccessRejectionDoesNotProbeAnOlderYear() throws Exception {
    startServer(
        Map.of(
            get(ROOT_REQUEST),
            response(200, rootIndex(2025, 2026)),
            get(prefixRequest(2026)),
            response(429, "rate limited")));

    assertThatThrownBy(() -> supplier().getLatest(EnumSet.of(EntityType.ARTIST)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("HTTP 429");
    assertThat(requests)
        .containsExactly(get(ROOT_REQUEST), get(prefixRequest(2026)));
  }

  @Test
  void manifestAccessRejectionStopsBeforeObjectMetadata() throws Exception {
    LocalDate date = LocalDate.of(2026, 7, 1);
    startServer(
        Map.of(
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(entry(date, EntityType.ARTIST)), List.of(date))),
            get(downloadRequest(date)),
            response(403, "forbidden")));

    assertThatThrownBy(
            () -> supplier().getMonth(EnumSet.of(EntityType.ARTIST), YearMonth.from(date)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("HTTP 403");
    assertThat(requests)
        .containsExactly(get(prefixRequest(2026)), get(downloadRequest(date)));
  }

  @Test
  void objectMetadataAccessRejectionDoesNotRetryTheEntity() throws Exception {
    LocalDate date = LocalDate.of(2026, 7, 1);
    startServer(
        Map.of(
            get(prefixRequest(2026)),
            response(
                200,
                yearIndex(
                    List.of(entry(date, EntityType.ARTIST)), List.of(date))),
            get(downloadRequest(date)),
            response(200, manifestLine(date, EntityType.ARTIST, ARTIST_CHECKSUM)),
            head(dumpRequest(date, EntityType.ARTIST)),
            headResponse(429, null)));

    assertThatThrownBy(
            () -> supplier().getMonth(EnumSet.of(EntityType.ARTIST), YearMonth.from(date)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("HTTP 429");
    assertThat(requests)
        .containsExactly(
            get(prefixRequest(2026)),
            get(downloadRequest(date)),
            head(dumpRequest(date, EntityType.ARTIST)));
  }

  @Test
  void selectedRequestsRejectInvalidInputsAndEmptyCatalogs() throws Exception {
    DefaultDumpSupplier supplier = new DefaultDumpSupplier();
    assertThatThrownBy(() -> supplier.getLatest(null))
        .isInstanceOf(InvalidArgumentException.class);
    assertThatThrownBy(() -> supplier.getLatest(Set.of()))
        .isInstanceOf(InvalidArgumentException.class);
    assertThatThrownBy(() -> supplier.getMonth(EnumSet.of(EntityType.ARTIST), null))
        .isInstanceOf(InvalidArgumentException.class);

    startServer(Map.of(get(ROOT_REQUEST), response(200, "<html></html>")));
    assertThatThrownBy(() -> supplier().getLatest(EnumSet.of(EntityType.ARTIST)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("contains no years");
  }

  @Test
  void interruptionsArePreservedForLatestAndMonth() throws Exception {
    DefaultDumpSupplier latest = spy(new DefaultDumpSupplier());
    doThrow(new InterruptedException("fixture"))
        .when(latest)
        .getDiscogsDataSource(DiscogsDumpUrls.PUBLIC_CATALOG_URI.toString());
    assertThatThrownBy(() -> latest.getLatest(EnumSet.of(EntityType.ARTIST)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("fixture");
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    Thread.interrupted();

    DefaultDumpSupplier month = spy(new DefaultDumpSupplier());
    String yearUrl =
        DiscogsDumpUrls.catalogYear(DiscogsDumpUrls.PUBLIC_CATALOG_URI, 2026).toString();
    doThrow(new InterruptedException("fixture"))
        .when(month)
        .getDiscogsDataSource(yearUrl);
    assertThatThrownBy(
            () -> month.getMonth(EnumSet.of(EntityType.ARTIST), YearMonth.of(2026, 7)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("fixture");
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }

  @Test
  void publicUrlsStayOnTheOfficialCatalogDownloadBoundary() {
    assertThat(
            DiscogsDumpUrls.dump(
                    DiscogsDumpUrls.PUBLIC_CATALOG_URI,
                    "data/2026/discogs_20260701_artists.xml.gz")
                .toString())
        .isEqualTo(
            "https://data.discogs.com/"
                + "?download=data%2F2026%2Fdiscogs_20260701_artists.xml.gz");
    assertThat(DiscogsDumpUrls.catalogYear(URI.create("https://example.test"), 2026).toString())
        .isEqualTo("https://example.test/?prefix=data%2F2026%2F");
    assertThat(DiscogsDumpUrls.manifestPath(LocalDate.of(2026, 7, 1)))
        .isEqualTo("data/2026/discogs_20260701_CHECKSUM.txt");
    assertThatThrownBy(() -> DiscogsDumpUrls.toUrl(URI.create("unknown://example.test")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be represented as a URL");
  }

  private DefaultDumpSupplier supplier() {
    return new DefaultDumpSupplier(serverUri());
  }

  private URI serverUri() {
    return URI.create(
        "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + "/");
  }

  private void startServer(Map<String, StubResponse> responses) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String query = exchange.getRequestURI().getRawQuery();
          String request = query == null ? ROOT_REQUEST : query;
          String key = exchange.getRequestMethod() + " " + request;
          requests.add(key);
          StubResponse response = responses.getOrDefault(key, response(404, "missing"));
          respond(exchange, response);
        });
    server.start();
  }

  private void respond(HttpExchange exchange, StubResponse response) throws IOException {
    if (response.contentLength() != null) {
      exchange
          .getResponseHeaders()
          .set("Content-Length", Long.toString(response.contentLength()));
    }
    if (HEAD_METHOD.equals(exchange.getRequestMethod())) {
      exchange.sendResponseHeaders(response.status(), -1);
      exchange.close();
      return;
    }
    byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(response.status(), body.length);
    try (var output = exchange.getResponseBody()) {
      output.write(body);
    }
  }

  private StubResponse response(int status, String body) {
    return new StubResponse(status, body, null);
  }

  private StubResponse headResponse(int status, Long contentLength) {
    return new StubResponse(status, "", contentLength);
  }

  private String rootIndex(int... years) {
    StringBuilder html = new StringBuilder("<html><body>");
    for (int year : years) {
      html.append("<a href=\"?prefix=data%2F")
          .append(year)
          .append("%2F\">")
          .append(year)
          .append("</a>");
    }
    return html.append("</body></html>").toString();
  }

  private String yearIndex(List<DumpEntry> entries, List<LocalDate> manifestDates) {
    StringBuilder html = new StringBuilder();
    for (LocalDate date : manifestDates) {
      String stamp = DATE_FORMATTER.format(date);
      html.append("<a href=\"?")
          .append(downloadRequest(date))
          .append("\">discogs_")
          .append(stamp)
          .append("_CHECKSUM.txt</a>\n");
    }
    for (DumpEntry entry : entries) {
      String stamp = DATE_FORMATTER.format(entry.date());
      String fileName = "discogs_" + stamp + "_" + entry.type() + "s.xml.gz";
      html.append(entry.date())
          .append(" 00:00:00 1 MB <a href=\"?")
          .append(dumpRequest(entry.date(), entry.type()))
          .append("\">")
          .append(fileName)
          .append("</a>\n");
    }
    return html.toString();
  }

  private DumpEntry entry(LocalDate date, EntityType type) {
    return new DumpEntry(date, type);
  }

  private String manifestLine(LocalDate date, EntityType type, String checksum) {
    return checksum
        + "  discogs_"
        + DATE_FORMATTER.format(date)
        + "_"
        + type
        + "s.xml.gz\n";
  }

  private String prefixRequest(int year) {
    return "prefix=data%2F" + year + "%2F";
  }

  private String downloadRequest(LocalDate date) {
    return "download=data%2F"
        + date.getYear()
        + "%2Fdiscogs_"
        + DATE_FORMATTER.format(date)
        + "_CHECKSUM.txt";
  }

  private String dumpRequest(LocalDate date, EntityType type) {
    return "download=data%2F"
        + date.getYear()
        + "%2Fdiscogs_"
        + DATE_FORMATTER.format(date)
        + "_"
        + type
        + "s.xml.gz";
  }

  private String get(String request) {
    return GET_METHOD + " " + request;
  }

  private String head(String request) {
    return HEAD_METHOD + " " + request;
  }

  private record DumpEntry(LocalDate date, EntityType type) {
  }

  private record StubResponse(int status, String body, Long contentLength) {
  }
}
