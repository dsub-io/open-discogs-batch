package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;

import io.dsub.discogs.batch.condition.RequiresDiscogsDataConnection;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.util.ResourceUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Slf4j
@ExtendWith(RequiresDiscogsDataConnection.class)
class DefaultDumpSupplierUnitTest {

  @RegisterExtension
  public LogSpy logSpy = new LogSpy();
  DefaultDumpSupplier dumpSupplier;

  @BeforeEach
  void setUp() {
    dumpSupplier = Mockito.spy(new DefaultDumpSupplier());
  }

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

  @Test
  void whenParseDumpList__ThenMustReturnValidList() throws IOException {
    // when
    List<DiscogsDump> dumpList = dumpSupplier.parseDumpList(getTestFile("DiscogsDataDump.xml"));

    // then
    assertAll(() -> Assertions.assertThat(dumpList).isNotEmpty());
  }

  @Test
  void whenParseDumpListWithEmptyOrNullString__ShouldReturnEmptyList() {
    // when
    List<DiscogsDump> result = dumpSupplier.parseDumpList("");

    // then
    Assertions.assertThat(result).isEmpty();
    assertThat(logSpy.getEvents().size()).isEqualTo(1);
    assertThat(logSpy.getEvents().get(0).getMessage()).contains("urlString cannot be blank");
  }

  @Test
  void whenParseDumpWithBlankFields__ShouldNotReturnIncompleteDump() {
    try (InputStream in = new FileInputStream(getTestFile("DiscogsDataMissingFieldsExample.xml"))) {
      doReturn(in).when(dumpSupplier).openStream(null);
      doReturn(null).when(dumpSupplier).getBucketURL();

      // when
      List<DiscogsDump> dumpList = dumpSupplier.parseDumpList(dumpSupplier.getBucketURL());

      // then
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
    } catch (IOException e) {
      fail(e);
    }
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
}
