package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.condition.RequiresDiscogsDataConnection;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@ExtendWith(RequiresDiscogsDataConnection.class)
class DefaultDumpSupplierUnitTest {

  static final String TEST_XML_DIRECTORY = "src/test/resources/test/";

  final DefaultDumpSupplier dumpSupplier = new DefaultDumpSupplier();

  @RegisterExtension public LogSpy logSpy = new LogSpy();

  @Test
  void whenGet__ThenReturnsNotEmptyListOfValidDiscogsDumps() {
    // when
    List<DiscogsDump> foundList = dumpSupplier.get();

    // then
    assertThat(dumpSupplier.get())
        .isNotNull()
        .satisfies(list -> assertThat(list.size()).isGreaterThan(0));

    foundList.forEach(
        item ->
            assertThat(item)
                .satisfies(dump -> assertThat(dump.getETag()).isNotNull().isNotBlank())
                .satisfies(dump -> assertThat(dump.getSize()).isNotNull().isGreaterThan(0))
                .satisfies(dump -> assertThat(dump.getUriString()).isNotNull().isNotBlank())
                .satisfies(dump -> assertThat(dump.getFileName()).matches("^[\\w_]+.xml.gz$"))
                .satisfies(dump -> assertThat(dump.getType()).isNotNull()));
  }

  @Test
  void whenGetBucketURL__ReturnsValidURL() {
    // when
    String url = dumpSupplier.getBucketURL();

    // then
    assertThat(url).isNotNull().isNotBlank().matches("^https://[\\w_.-]+[\\w_-].com$");
  }

  // todo: impl test
  @Test
  void whenParseDumpList__ThenMustReturnValidList() {
    // when
    dumpSupplier.parseDumpList(dumpSupplier.getBucketURL());
    // then
  }

  @Test
  void whenParseDumpListWithEmptyOrNullString__ThenReturnsNull() {
    // when
    List<DiscogsDump> result = dumpSupplier.parseDumpList(null);

    // then
    assertThat(result).isNull();
    assertThat(logSpy.getEvents().size()).isEqualTo(1);
    assertThat(logSpy.getEvents().get(0).getMessage()).contains("malformed url string:");

    // when
    result = dumpSupplier.parseDumpList("");

    // then
    assertThat(result).isNull();
    assertThat(logSpy.getEvents().size()).isEqualTo(2);
    assertThat(logSpy.getEvents().get(1).getMessage()).contains("malformed url string:");
  }

  @Test
  void whenParseDumpWithProperXml__ShouldParseTheValuesWithNoNullValues() {
    File file = Paths.get(TEST_XML_DIRECTORY + "DiscogsDataValidExample.xml").toFile();

    assertDoesNotThrow(
        () ->
            readDumpWithParseDumpMethod(file)
                .forEach(
                    discogsDump -> {
                      assertThat(discogsDump.getCreatedAt()).isNotNull();
                      assertThat(discogsDump.getSize()).isNotNull();
                      assertThat(discogsDump.getETag()).isNotNull();
                      assertThat(discogsDump.getUriString()).isNotNull();
                      assertThat(discogsDump.getType()).isNotNull();
                    }));
  }

  @Test
  void whenParseDumpWithBlankFields__ShouldNotReturnIncompleteDump() {
    File file = Paths.get(TEST_XML_DIRECTORY + "DiscogsDataMissingFieldsExample.xml").toFile();

    assertDoesNotThrow(
        () ->
            readDumpWithParseDumpMethod(file)
                .forEach(
                    discogsDump -> {
                      if (discogsDump == null) {
                        return;
                      }
                      assertThat(discogsDump.getCreatedAt()).isNotNull();
                      assertThat(discogsDump.getSize()).isNotNull();
                      assertThat(discogsDump.getETag()).isNotNull();
                      assertThat(discogsDump.getUriString()).isNotNull();
                      assertThat(discogsDump.getType()).isNotNull();
                    }));
  }

  @Test
  void whenGetSizeCalledWithInvalidString__ShouldThrowInvalidArgumentException__WithValidMessage() {
    File file = Paths.get(TEST_XML_DIRECTORY + "ParseLongValueTestMalformedExample.xml").toFile();
    assertDoesNotThrow(
        () -> {
          try (InputStream in = new FileInputStream(file)) {
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
    File file = Paths.get(TEST_XML_DIRECTORY + "ParseLongValueTestValidExample.xml").toFile();

    try (InputStream in = new FileInputStream(file)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList sizes = document.getElementsByTagName("Size");
      for (int i = 0; i < sizes.getLength(); i++) {
        Node node = sizes.item(i);
        Long value = Long.parseLong(node.getTextContent());
        assertThat(dumpSupplier.getSize(node)).isEqualTo(value);
      }
    }
  }

  @Test
  void whenUTCLastModifiedMethodCalledWithValidEntry__ShouldReturnNonNullValidValue()
      throws IOException, ParserConfigurationException, SAXException {

    File file = Paths.get(TEST_XML_DIRECTORY + "DiscogsDataValidExample.xml").toFile();
    try (InputStream in = new FileInputStream(file)) {
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
    }
  }

  @Test
  void whenUTCLastModifiedMethodCalledWithMalformedEntry__()
      throws IOException, ParserConfigurationException, SAXException {

    File file =
        Paths.get(TEST_XML_DIRECTORY + "UTCLastModifiedMethodTestMalformedExample.xml").toFile();
    try (InputStream in = new FileInputStream(file)) {
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
    File file = Paths.get(TEST_XML_DIRECTORY + "GetTypeTestMalformedEntryExample.xml").toFile();

    try (InputStream in = new FileInputStream(file)) {
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
  void whenGetTypeCalledWithValidEntry__ShouldReturnValidResult()
      throws IOException, ParserConfigurationException, SAXException {
    File file = Paths.get(TEST_XML_DIRECTORY + "DiscogsDataValidExample.xml").toFile();

    try (InputStream in = new FileInputStream(file)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int pIdx = 0; pIdx < contents.getLength(); pIdx++) {
        for (int cIdx = 0; cIdx < contents.item(pIdx).getChildNodes().getLength(); cIdx++) {
          Node node = contents.item(pIdx).getChildNodes().item(cIdx);

          // when
          if (node.getNodeName().equals("Key")) {

            // then
            assertThat(dumpSupplier.getType(node)).isIn((Object[]) DumpType.values());
          }
        }
      }
    }
  }

  @Test
  void whenIsKnownNodeTypeCalledWithValidExample__ShouldReturnProperResponse()
      throws IOException, ParserConfigurationException, SAXException {
    File file = Paths.get(TEST_XML_DIRECTORY + "DiscogsDataValidExample.xml").toFile();
    try (InputStream in = new FileInputStream(file)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int pIdx = 0; pIdx < contents.getLength(); pIdx++) {
        for (int cIdx = 0; cIdx < contents.item(pIdx).getChildNodes().getLength(); cIdx++) {
          Node n = contents.item(pIdx).getChildNodes().item(cIdx);
          String nodeName = n.getNodeName();
          if (nodeName.equals("#text") || nodeName.equals("StorageClass")) {
            assertThat(dumpSupplier.isKnownNodeType(n)).isFalse();
          } else {
            assertThat(dumpSupplier.isKnownNodeType(n)).isTrue();
          }
        }
      }
    }
  }

  @Test
  void whenIsXmlGZipEntryCalledWithValidXml__ShouldReturnTrueForAllValidEntries()
      throws IOException, ParserConfigurationException, SAXException {
    File file = Paths.get(TEST_XML_DIRECTORY + "DiscogsDataValidExample.xml").toFile();
    try (InputStream in = new FileInputStream(file)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int i = 0; i < contents.getLength(); i++) {
        assertThat(dumpSupplier.isXmlGzipEntry(contents.item(i).getChildNodes())).isTrue();
      }
    }
  }

  @Test
  void whenIsXmlGZipEntryCalledWithMissingField__ShouldReturnValue()
      throws IOException, ParserConfigurationException, SAXException {
    File file = Paths.get(TEST_XML_DIRECTORY + "IsXmlGZipEntryInvalidExample.xml").toFile();
    try (InputStream in = new FileInputStream(file)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int i = 0; i < contents.getLength(); i++) {
        assertThat(dumpSupplier.isXmlGzipEntry(contents.item(i).getChildNodes())).isFalse();
      }
    }
  }

  private List<DiscogsDump> readDumpWithParseDumpMethod(File file)
      throws IOException, ParserConfigurationException, SAXException {
    List<DiscogsDump> dumpList = new ArrayList<>();
    try (InputStream in = new FileInputStream(file)) {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(in);
      NodeList contents = document.getElementsByTagName("Contents");
      for (int i = 0; i < contents.getLength(); i++) {
        NodeList nodeList = contents.item(i).getChildNodes();
        DiscogsDump discogsDump = dumpSupplier.parseDump(nodeList);
        dumpList.add(discogsDump);
      }
    }
    return dumpList;
  }
}
