package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.exception.UnknownDumpTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SimpleDumpFetcher implements DumpFetcher {

    private static final String DISCOGS_DATA_BASE_URL = "https://data.discogs.com/";
    public static String LAST_KNOWN_BUCKET_URL = "https://discogs-data.s3-us-west-2.amazonaws.com";

    private static boolean isWithinValidRange(LocalDateTime targetDateTime) {
        LocalDate that = LocalDate.of(2018, 1, 1);
        LocalDate targetDate = targetDateTime.toLocalDate();
        return (targetDate.isEqual(that) || targetDate.isAfter(that));
    }

    private static DumpType getDumpType(String dumpKey) {
        if (dumpKey.contains("releases")) return DumpType.RELEASE;
        if (dumpKey.contains("artists")) return DumpType.ARTIST;
        if (dumpKey.contains("labels")) return DumpType.LABEL;
        if (dumpKey.contains("masters")) return DumpType.MASTER;
        throw new UnknownDumpTypeException("failed to parse dump type from " + dumpKey);
    }

    public List<DumpItem> getDiscogsDumps() {

        List<DumpItem> dumpList = new ArrayList<>();
        URL url;

        try {
            url = new URL(LAST_KNOWN_BUCKET_URL);
        } catch (MalformedURLException e) {
            log.error("Malformed url detected. Returning empty container...");
            return new ArrayList<>();
        }

        try (InputStream in = url.openStream()) {

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newDefaultInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            Document document = builder.parse(in);
            NodeList contents = document.getElementsByTagName("Contents");

            for (int i = 0; i < contents.getLength(); i++) {
                Node contentNode = contents.item(i);
                NodeList dataNodeList = contentNode.getChildNodes();

                if (dataNodeList.item(0).getTextContent().matches("\\S+.xml.gz")) {
                    DumpItem dump = parseDump(dataNodeList);
                    if (dump != null &&
                            dump.getLastModified() != null &&
                            isWithinValidRange(dump.getLastModified())) {
                        dumpList.add(dump);
                    }
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            log.error("Dump item size has invalid data. Skipping..");
        }
        return dumpList;
    }

    public DumpItem parseDump(NodeList dataNodeList) {
        DumpType dumpType = null;
        String uri = "";
        LocalDateTime lastModified = null;
        String etag = "";
        long size = 0L;

        try {
            for (int j = 0; j < dataNodeList.getLength(); j++) {
                Node data = dataNodeList.item(j);

                switch (data.getNodeName()) {
                    case "Key":
                        uri = data.getTextContent();
                        dumpType = getDumpType(uri);
                        break;
                    case "LastModified":
                        lastModified = OffsetDateTime
                                .parse(data.getTextContent())
                                .withOffsetSameInstant(ZoneOffset.UTC)
                                .toLocalDateTime();
                        break;
                    case "ETag":
                        etag = data.getTextContent().replace("\"", "");
                        break;
                    case "Size":
                        size = Long.parseLong(data.getTextContent());
                        break;
                }
            }
        } catch (NumberFormatException e) {
            log.error("dump item " + uri + " has invalid data. skipping item...");
            return null;
        } catch (UnknownDumpTypeException e) {
            log.error(e.getMessage() + ". skipping item...");
            return null;
        } catch (DateTimeParseException e) {
            log.error("dump item " + uri + " has invalid LastModified entry. skipping item...");
        }

        return DumpItem.builder()
                .uri(uri)
                .dumpType(dumpType)
                .eTag(etag)
                .size(size)
                .lastModified(lastModified)
                .build();
    }
}
