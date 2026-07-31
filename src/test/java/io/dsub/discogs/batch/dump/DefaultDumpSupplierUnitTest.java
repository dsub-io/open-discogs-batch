package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Slf4j
class DefaultDumpSupplierUnitTest {

  @RegisterExtension
  public LogSpy logSpy = new LogSpy();
  DefaultDumpSupplier dumpSupplier;

  @BeforeEach
  void setUp() {
    dumpSupplier = Mockito.spy(new DefaultDumpSupplier());
  }

  @Test
  void whenParseDumpList__ThenMustReturnValidList() throws IOException {
    // when
    List<DiscogsDump> dumpList = dumpSupplier.parseDumpList(getTestFile("DiscogsDataDump.xml"));

    // then
    assertAll(() -> Assertions.assertThat(dumpList).isNotEmpty());
  }

  @Test
  void whenParseYearIndexUrls__ThenReturnsEveryYearDirectory() throws IOException {
    List<String> result =
        dumpSupplier.parseYearIndexUrls(readTestFile("DiscogsDataIndex.html"));

    assertThat(result)
        .containsExactly(
            "https://data.discogs.com/?prefix=data%2F2008%2F",
            "https://data.discogs.com/?prefix=data%2F2025%2F",
            "https://data.discogs.com/?prefix=data%2F2026%2F");
  }

  @Test
  void whenParseHtmlDumpList__ThenReturnsDownloadableDumps() throws IOException {
    List<DiscogsDump> result =
        dumpSupplier.parseHtmlDumpList(readTestFile("DiscogsData2026.html"));

    assertThat(result).hasSize(4);
    assertThat(result)
        .extracting(DiscogsDump::getType)
        .containsExactly(
            EntityType.ARTIST, EntityType.LABEL, EntityType.MASTER, EntityType.RELEASE);
    assertThat(result)
        .allSatisfy(
            dump -> {
              assertThat(dump.getETag()).isEqualTo(dump.getUriString());
              assertThat(dump.getLastModifiedAt()).isEqualTo(LocalDate.of(2026, 7, 1));
              assertThat(dump.getSize()).isPositive();
              assertThat(dump.getUrl().toString())
                  .startsWith(
                      "https://discogs-data-dumps.s3.us-west-2.amazonaws.com/data/2026/");
              assertThat(dump.getChecksumUrl().toString())
                  .isEqualTo(
                      "https://data.discogs.com/"
                          + "?download=data%2F2026%2Fdiscogs_20260701_CHECKSUM.txt");
            });
  }

  @Test
  void whenParseChecksumUrls__ThenAssociatesManifestByDumpDate() throws IOException {
    assertThat(dumpSupplier.parseChecksumUrls(readTestFile("DiscogsData2026.html")))
        .containsOnlyKeys(LocalDate.of(2026, 7, 1));
  }

  @Test
  void whenDumpHasNoMatchingChecksum__ThenSkipsIt() {
    String html =
        "2026-07-01 00:00:00 1 MB "
            + "<a href=\"?download=data%2F2026%2Fdiscogs_20260701_artists.xml.gz\">"
            + "discogs_20260701_artists.xml.gz</a>";

    assertThat(dumpSupplier.parseHtmlDumpList(html)).isEmpty();
    assertThat(logSpy.getEvents())
        .anyMatch(event -> event.getMessage().contains("without a matching checksum"));
  }

  @Test
  void whenGet__ThenFetchesRootAndEveryYearIndex() throws Exception {
    String rootIndex = readTestFile("DiscogsDataIndex.html");
    String yearIndex = readTestFile("DiscogsData2026.html");
    doReturn(rootIndex)
        .when(dumpSupplier)
        .getDiscogsDataSource("https://data.discogs.com/");
    for (String yearUrl : dumpSupplier.parseYearIndexUrls(rootIndex)) {
      doReturn(yearIndex).when(dumpSupplier).getDiscogsDataSource(yearUrl);
    }

    assertThat(dumpSupplier.get()).hasSize(12);
  }

  @Test
  void whenOneYearIndexFails__ThenReturnsOtherYears() throws Exception {
    String rootIndex = readTestFile("DiscogsDataIndex.html");
    String yearIndex = readTestFile("DiscogsData2026.html");
    List<String> yearUrls = dumpSupplier.parseYearIndexUrls(rootIndex);
    doReturn(rootIndex)
        .when(dumpSupplier)
        .getDiscogsDataSource("https://data.discogs.com/");
    doReturn(yearIndex).when(dumpSupplier).getDiscogsDataSource(yearUrls.get(0));
    doThrow(new IOException("unavailable"))
        .when(dumpSupplier)
        .getDiscogsDataSource(yearUrls.get(1));
    doReturn(yearIndex).when(dumpSupplier).getDiscogsDataSource(yearUrls.get(2));

    assertThat(dumpSupplier.get()).hasSize(8);
    assertThat(logSpy.getEvents())
        .anyMatch(event -> event.getMessage().contains("failed to fetch Discogs data index"));
  }

  @Test
  void whenRootIndexFails__ThenUsesManifestFallback() throws Exception {
    DiscogsDump fallbackDump =
        new DiscogsDump(
            "data/2026/discogs_20260701_artists.xml.gz",
            EntityType.ARTIST,
            "data/2026/discogs_20260701_artists.xml.gz",
            -1L,
            LocalDate.of(2026, 7, 1),
            null);
    doThrow(new IOException("unavailable"))
        .when(dumpSupplier)
        .getDiscogsDataSource("https://data.discogs.com/");
    doReturn(List.of(fallbackDump))
        .when(dumpSupplier)
        .getLatestCompleteDumpsFromManifests();

    assertThat(dumpSupplier.get()).containsExactly(fallbackDump);
    assertThat(logSpy.getEvents())
        .anyMatch(event -> event.getMessage().contains("failed to fetch the Discogs data index"));
  }

  @Test
  void whenLatestManifestIsMissing__ThenUsesPreviousCompleteMonth() throws Exception {
    String augustManifestUrl = manifestUrl("20260801");
    String julyManifestUrl = manifestUrl("20260701");
    doReturn(LocalDate.of(2026, 8, 1)).when(dumpSupplier).getCurrentUtcDate();
    doReturn(Optional.empty())
        .when(dumpSupplier)
        .getDiscogsManifestSource(augustManifestUrl);
    doReturn(Optional.of(completeManifest("20260701")))
        .when(dumpSupplier)
        .getDiscogsManifestSource(julyManifestUrl);

    List<DiscogsDump> result = dumpSupplier.getLatestCompleteDumpsFromManifests();

    assertThat(result)
        .hasSize(4)
        .extracting(DiscogsDump::getType)
        .containsExactly(EntityType.values());
    assertThat(result)
        .allSatisfy(
            dump -> {
              assertThat(dump.getLastModifiedAt()).isEqualTo(LocalDate.of(2026, 7, 1));
              assertThat(dump.getSize()).isEqualTo(-1L);
              assertThat(dump.getETag()).isEqualTo(dump.getUriString());
              assertThat(dump.getUrl().toString())
                  .startsWith(
                      "https://discogs-data-dumps.s3.us-west-2.amazonaws.com/data/2026/");
              assertThat(dump.getChecksumUrl().toString()).isEqualTo(julyManifestUrl);
            });
    verify(dumpSupplier, times(1)).getDiscogsManifestSource(augustManifestUrl);
    verify(dumpSupplier, times(1)).getDiscogsManifestSource(julyManifestUrl);
  }

  @Test
  void whenLatestManifestMissesOneDomain__ThenFillsOnlyThatDomainFromPreviousMonth()
      throws Exception {
    String julyManifestUrl = manifestUrl("20260701");
    String juneManifestUrl = manifestUrl("20260601");
    doReturn(LocalDate.of(2026, 7, 31)).when(dumpSupplier).getCurrentUtcDate();
    doReturn(
            Optional.of(
                completeManifest("20260701")
                    .replace(manifestLine("20260701", EntityType.LABEL), "")))
        .when(dumpSupplier)
        .getDiscogsManifestSource(julyManifestUrl);
    doReturn(Optional.of(completeManifest("20260601")))
        .when(dumpSupplier)
        .getDiscogsManifestSource(juneManifestUrl);

    List<DiscogsDump> result = dumpSupplier.getLatestCompleteDumpsFromManifests();

    assertThat(result)
        .hasSize(4)
        .allSatisfy(
            dump ->
                assertThat(dump.getLastModifiedAt())
                    .isEqualTo(
                        dump.getType() == EntityType.LABEL
                            ? LocalDate.of(2026, 6, 1)
                            : LocalDate.of(2026, 7, 1)));
    assertThat(logSpy.getEvents())
        .anyMatch(
            event ->
                event
                    .getFormattedMessage()
                    .contains("manifest for 2026-07-01 does not cover every unresolved entity"));
  }

  @Test
  void whenManifestAccessIsRejected__ThenDoesNotRetryOlderMonths() throws Exception {
    String julyManifestUrl = manifestUrl("20260701");
    doReturn(LocalDate.of(2026, 7, 31)).when(dumpSupplier).getCurrentUtcDate();
    doThrow(new IOException("HTTP 403"))
        .when(dumpSupplier)
        .getDiscogsManifestSource(julyManifestUrl);

    assertThrows(IOException.class, dumpSupplier::getLatestCompleteDumpsFromManifests);
    verify(dumpSupplier, times(1)).getDiscogsManifestSource(julyManifestUrl);
  }

  @Test
  void whenRootIndexIsInterrupted__ThenRestoresInterruptStatus() throws Exception {
    doThrow(new InterruptedException("interrupted"))
        .when(dumpSupplier)
        .getDiscogsDataSource("https://data.discogs.com/");

    try {
      assertThat(dumpSupplier.get()).isEmpty();
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void whenHtmlContainsMalformedDumpEntries__ThenSkipsThem() {
    String html =
        "2026-07-01 00:00:00 1 MB "
            + "<a href=\"?download=data%2F2026%2Fdiscogs_20261301_artists.xml.gz\">"
            + "discogs_20261301_artists.xml.gz</a>\n"
            + "2026-07-01 00:00:00 1 MB "
            + "<a href=\"?download=data%2F2026%2Fdiscogs_20260701_unknown.xml.gz\">"
            + "discogs_20260701_unknown.xml.gz</a>\n"
            + "2026-07-01 00:00:00 1 MB "
            + "<a href=\"?download=data%2F2026%2Fdiscogs_20260701_artists.xml.gz\">"
            + "discogs_20260701_labels.xml.gz</a>";

    assertThat(dumpSupplier.parseHtmlDumpList(html)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
      "1, B, 1",
      "1, KB, 1024",
      "1.5, MB, 1572864",
      "2, GB, 2147483648",
      "1, TB, 1099511627776"
  })
  void whenParseDisplaySize__ThenReturnsBytes(String value, String unit, long expected) {
    assertThat(dumpSupplier.parseDisplaySize(value, unit)).isEqualTo(expected);
  }

  @Test
  void whenParseDisplaySizeWithMalformedValue__ThenThrows() {
    assertThrows(
        InvalidArgumentException.class, () -> dumpSupplier.parseDisplaySize("not-a-size", "MB"));
    assertThrows(
        InvalidArgumentException.class, () -> dumpSupplier.parseDisplaySize("1", "unknown"));
    assertThrows(
        InvalidArgumentException.class,
        () -> dumpSupplier.parseDisplaySize("999999999999999999", "TB"));
  }

  @Test
  void whenHtmlIsBlank__ThenIndexParsersReturnEmptyLists() {
    assertThat(dumpSupplier.parseYearIndexUrls("")).isEmpty();
    assertThat(dumpSupplier.parseYearIndexUrls(null)).isEmpty();
    assertThat(dumpSupplier.parseHtmlDumpList("")).isEmpty();
    assertThat(dumpSupplier.parseHtmlDumpList(null)).isEmpty();
    assertThat(dumpSupplier.parseChecksumUrls("")).isEmpty();
    assertThat(dumpSupplier.parseChecksumUrls(null)).isEmpty();
  }

  @Test
  void whenParseDumpWithBlankFields__ShouldNotReturnIncompleteDump()
      throws FileNotFoundException {
    List<DiscogsDump> dumpList =
        dumpSupplier.parseDumpList(getTestFile("DiscogsDataMissingFieldsExample.xml"));

    assertAll(
        () -> Assertions.assertThat(dumpList).isNotEmpty(),
        () ->
            assertAll(
                () -> {
                  for (DiscogsDump dump : dumpList) {
                    assertThat(dump.getUriString()).isNotBlank();
                    assertThat(dump.getLastModifiedAt()).isNotNull();
                    assertThat(dump.getSize()).isNotNull();
                    assertThat(dump.getETag()).isNotNull();
                    assertThat(dump.getType()).isNotNull();
                  }
                }));
  }

  @Test
  void whenGetSizeCalledWithInvalidString__ShouldThrowInvalidArgumentException__WithValidMessage() {
    assertDoesNotThrow(
        () -> {
          try (InputStream in =
              new FileInputStream(getTestFile("ParseLongValueTestMalformedExample.xml"))) {
            DocumentBuilder documentBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(in);
            NodeList sizes = document.getElementsByTagName("Size");
            String msg =
                assertThrows(
                    InvalidArgumentException.class, () -> dumpSupplier.getSize(sizes.item(0)))
                    .getMessage();
            assertThat(msg).isEqualTo("failed to parse [] into long value");

            msg =
                assertThrows(
                    InvalidArgumentException.class, () -> dumpSupplier.getSize(sizes.item(1)))
                    .getMessage();
            assertThat(msg).isEqualTo("failed to parse [d] into long value");

            msg =
                assertThrows(
                    InvalidArgumentException.class, () -> dumpSupplier.getSize(sizes.item(2)))
                    .getMessage();

            assertThat(msg).isEqualTo("failed to parse [3323d] into long value");

            msg =
                assertThrows(
                    InvalidArgumentException.class, () -> dumpSupplier.getSize(sizes.item(3)))
                    .getMessage();
            assertThat(msg).isEqualTo("failed to parse [33211d22!!#] into long value");
          }
        });
  }

  @Test
  void whenGetSizeCalledWithValidString__ShouldReturnValidValues()
      throws IOException, ParserConfigurationException, SAXException {
    try (InputStream in = new FileInputStream(getTestFile("ParseLongValueTestValidExample.xml"))) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList sizes = document.getElementsByTagName("Size");
      for (int i = 0; i < sizes.getLength(); i++) {
        Node node = sizes.item(i);
        Long value = Long.parseLong(node.getTextContent());
        assertThat(dumpSupplier.getSize(node)).isEqualTo(value);
      }
    } catch (InvalidArgumentException e) {
      fail(e);
    }
  }

  @Test
  void whenUTCLastModifiedMethodCalledWithValidEntry__ShouldReturnNonNullValidValue()
      throws IOException, ParserConfigurationException, SAXException {
    try (InputStream in = new FileInputStream(getTestFile("DiscogsDataDump.xml"))) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int pIdx = 0; pIdx < contents.getLength(); pIdx++) {
        for (int cIdx = 0; cIdx < contents.item(pIdx).getChildNodes().getLength(); cIdx++) {
          Node node = contents.item(pIdx).getChildNodes().item(cIdx);
          // when
          if (node.getNodeName().equals("LastModified")) {
            LocalDateTime expected =
                OffsetDateTime.parse(node.getTextContent())
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
            // then
            LocalDateTime parseResult = dumpSupplier.getUTCLastModified(node);
            assertThat(expected).isEqualTo(parseResult);
          }
        }
      }
    } catch (InvalidArgumentException e) {
      fail(e);
    }
  }

  @Test
  void whenUTCLastModifiedMethodCalledWithMalformedEntry__()
      throws IOException, ParserConfigurationException, SAXException {
    try (InputStream in =
        new FileInputStream(getTestFile("UTCLastModifiedMethodTestMalformedExample.xml"))) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int pIdx = 0; pIdx < contents.getLength(); pIdx++) {
        for (int cIdx = 0; cIdx < contents.item(pIdx).getChildNodes().getLength(); cIdx++) {
          Node node = contents.item(pIdx).getChildNodes().item(cIdx);
          // when
          if (node.getNodeName().equals("LastModified")) {
            // then
            String msg =
                assertThrows(
                    InvalidArgumentException.class, () -> dumpSupplier.getUTCLastModified(node))
                    .getMessage();

            if (node.getTextContent() == null || node.getTextContent().isBlank()) {
              assertThat(msg)
                  .isEqualTo("cannot parse null or blank string into LocalDateTime instance");
            } else {
              assertThat(msg)
                  .isEqualTo("failed to parse " + node.getTextContent() + " to OffsetDateTime");
            }
          }
        }
      }
    }
  }

  @Test
  void whenGetTypeCalledWithMalformedEntry__ShouldThrowInvalidArgumentException__WithValidMessage()
      throws IOException, ParserConfigurationException, SAXException {
    try (InputStream in =
        new FileInputStream(getTestFile("GetTypeTestMalformedEntryExample.xml"))) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int pIdx = 0; pIdx < contents.getLength(); pIdx++) {
        for (int cIdx = 0; cIdx < contents.item(pIdx).getChildNodes().getLength(); cIdx++) {
          Node node = contents.item(pIdx).getChildNodes().item(cIdx);

          // when
          if (node.getNodeName().equals("Key")) {

            // then
            String msg =
                assertThrows(InvalidArgumentException.class, () -> dumpSupplier.getType(node))
                    .getMessage();
            assertThat(msg)
                .isEqualTo("unknown dump type found for node content: data/2008/discogs_20080309");
          }
        }
      }
    }
  }

  @Test
  void whenIsKnownNodeTypeCalledWithValidExample__ShouldReturnProperResponse()
      throws IOException, ParserConfigurationException, SAXException {
    try (InputStream in = new FileInputStream(getTestFile("DiscogsDataDump.xml"))) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int pIdx = 0; pIdx < contents.getLength(); pIdx++) {
        for (int cIdx = 0; cIdx < contents.item(pIdx).getChildNodes().getLength(); cIdx++) {
          Node n = contents.item(pIdx).getChildNodes().item(cIdx);
          String nodeName = n.getNodeName();
          if (nodeName.equals("#text") || nodeName.equals("StorageClass") || nodeName
              .equals("LastModified")) {
            assertThat(dumpSupplier.isKnownNodeType(n)).isFalse();
          } else {
            assertThat(dumpSupplier.isKnownNodeType(n)).isTrue();
          }
        }
      }
    }
  }

  private File getTestFile(String filename) throws FileNotFoundException {
    return ResourceUtils.getFile("classpath:test/" + filename);
  }

  private String readTestFile(String filename) throws IOException {
    return Files.readString(getTestFile(filename).toPath());
  }

  private String completeManifest(String dateStamp) {
    StringBuilder manifest = new StringBuilder();
    for (EntityType type : EntityType.values()) {
      manifest.append(manifestLine(dateStamp, type));
    }
    return manifest.toString();
  }

  private String manifestLine(String dateStamp, EntityType type) {
    return "a".repeat(64)
        + "  discogs_"
        + dateStamp
        + "_"
        + type
        + "s.xml.gz\n";
  }

  private String manifestUrl(String dateStamp) {
    return "https://data.discogs.com/?download=data%2F"
        + dateStamp.substring(0, 4)
        + "%2Fdiscogs_"
        + dateStamp
        + "_CHECKSUM.txt";
  }
}
