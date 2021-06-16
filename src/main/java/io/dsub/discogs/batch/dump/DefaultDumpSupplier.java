package io.dsub.discogs.batch.dump;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Slf4j
@Getter
@Component
public class DefaultDumpSupplier implements DumpSupplier {

  private static final Pattern XML_GZ_PATTERN =
      Pattern.compile("^[\\w/_-]+.xml.gz$", Pattern.CASE_INSENSITIVE);
  private static final Pattern ARTIST = Pattern.compile(".*artists.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern RELEASE_ITEM =
      Pattern.compile(".*releases.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern MASTER = Pattern.compile(".*masters.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern LABEL = Pattern.compile(".*labels.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern BUCKET_VAR_DECLARATION_PATTERN =
      Pattern.compile(".*bucket_url.*", Pattern.CASE_INSENSITIVE);

  private static final String CONTENTS_TAG_NAME = "Contents";
  private static final String KEY = "Key";
  private static final String LAST_MODIFIED = "LastModified";
  private static final String ETAG = "ETag";
  private static final String SIZE = "Size";

  private static final List<String> KNOWN_NODE_TYPES = List.of(KEY, LAST_MODIFIED, ETAG, SIZE);

  private static final String DISCOGS_DATA_URL = "http://data.discogs.com/";
  private String lastKnownBucketUrl = "https://discogs-data.s3-us-west-2.amazonaws.com";

  /**
   * Implementation for {@link Supplier} that supplies parsed dump list. If something goes wrong it
   * will simply return null as indicating failure of parsing.
   *
   * @return parsed discogs dump list, or null if we failed to locate.
   */
  @Override
  public List<DiscogsDump> get() {

    List<DiscogsDump> parsedList = parseDumpList(getLastKnownBucketUrl()); // initial parse
    if (parsedList == null || parsedList.isEmpty()) { // if failed...

      String freshBucketUrl = getBucketURL(); // fetch new bucket url from the official page
      if (freshBucketUrl == null || freshBucketUrl.isBlank()) { // failed again....
        return null; // failed, hence return null.
      }
      lastKnownBucketUrl = freshBucketUrl;
      parsedList = parseDumpList(getLastKnownBucketUrl()); // second time parse...
    }

    return parsedList.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList()); // this may be null or actual parse result.
  }

  /**
   * Parse bucket url presented in http://data.discogs.com. When called, this will fetch the html as
   * a stream of strings, then parse accordingly. As of something may go wonky, this will retry 3
   * times for parsing...
   *
   * @return bucket url if success, else return null.
   */
  protected String getBucketURL() {
    Stream<String> resultStream;
    int retryCount = 0;
    String bucketURL = null;

    while (retryCount < 3 && bucketURL == null) { // retry future.get() method three times max.
      try {
        resultStream = getDiscogsDataSourceStream();
        AtomicReference<String> bucketUrlRef = new AtomicReference<>();
        resultStream
            .map(String::trim)
            .filter(s -> BUCKET_VAR_DECLARATION_PATTERN.matcher(s).matches())
            .findFirst()
            .ifPresent( // we found something that
                s -> {
                  s = StringUtils.trimAllWhitespace(s);
                  String fin = s.substring(s.indexOf('\'') + 1, s.lastIndexOf('\''));
                  if (fin.startsWith("//")) {
                    fin = "https:" + fin;
                  } else {
                    fin = "https://" + fin;
                  }
                  bucketUrlRef.set(fin);
                });
        // get referenced value.
        bucketURL = bucketUrlRef.get();
      } catch (ExecutionException | InterruptedException e) {
        retryCount++;
      }
    }
    return bucketURL;
  }

  protected Stream<String> getDiscogsDataSourceStream()
      throws ExecutionException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().build();

    // make GET request to
    CompletableFuture<HttpResponse<Stream<String>>> future =
        client.sendAsync(
            HttpRequest.newBuilder().uri(URI.create(DISCOGS_DATA_URL)).GET().build(),
            HttpResponse.BodyHandlers.ofLines());
    while (!future.isDone()) {
      Thread.onSpinWait();
    }
    return future.get().body();
  }

  /**
   * Parses list of discogs dump from given url string. If failed to parse the dump, this will
   * simply return NULL value.
   *
   * @return parse result, or null if anything goes wrong.
   */
  protected List<DiscogsDump> parseDumpList(String urlString) {
    try (InputStream in = openStream(urlString)) {
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newDefaultInstance();
      DocumentBuilder builder = builderFactory.newDocumentBuilder();

      Document document = builder.parse(in);
      NodeList contents = document.getElementsByTagName(CONTENTS_TAG_NAME);

      List<DiscogsDump> parseResult = new ArrayList<>();

      // loop through the NodeList content.
      for (int i = 0; i < contents.getLength(); i++) {
        Node contentNode = contents.item(i);
        NodeList dataNodeList = contentNode.getChildNodes();
        if (isXmlGzipEntry(dataNodeList)) {
          parseResult.add(parseDump(dataNodeList));
        }
      }
      return parseResult.stream().filter(Objects::nonNull).collect(Collectors.toList());
    } catch (MalformedURLException e) {
      log.error("malformed url string: {" + urlString + "}");
    } catch (IOException e) {
      log.error("found IOException for {" + e.getMessage() + "}");
    } catch (ParserConfigurationException e) {
      log.error("parser config has error: {" + e.getMessage() + "}");
    } catch (SAXException e) {
      log.error("found SAXException for {" + e.getMessage() + "}");
    }
    return null;
  }

  protected InputStream openStream(String url) throws IOException {
    return new URL(url).openStream();
  }

  /**
   * Parses {@link NodeList} into a {@link DiscogsDump}. If any info is missing from the NodeList,
   * it will naturally return as NULL.
   *
   * @param nodeList NodeList that contains the required information.
   * @return Constructed DiscogsDump from the given info.
   */
  protected DiscogsDump parseDump(NodeList nodeList) {

    // filter the nodeList as we do not require entire list
    List<Node> targetNodes =
        IntStream.range(0, nodeList.getLength())
            .mapToObj(nodeList::item)
            .filter(this::isKnownNodeType) // must be known type
            .filter(item -> item.getTextContent() != null) // must have content
            //            .filter(item -> item.getTextContent())// must have content
            .collect(Collectors.toList()); // conclude

    // if nodes has any missing field...
    if (targetNodes.size() < KNOWN_NODE_TYPES.size()) {
      return null;
    }

    // set object references for each required entries.
    DumpType type = null;
    String uri = null;
    LocalDateTime lastModified = null;
    String etag = null;
    Long size = null;
    LocalDate createdAt = null;
    URL url = null;

    try {
      // loop through the target nodes.
      for (Node node : targetNodes) {
        String content = node.getTextContent();
        if (content == null || content.isEmpty()) {
          return null;
        }
        switch (node.getNodeName()) {
          case KEY:
            uri = content; // formatted as 'data/{year}/{file_name}'
            url = new URL(lastKnownBucketUrl + "/" + uri);
            type = getType(node); // parse the last part of the uri.
            createdAt = parseCreatedAt(content);
            break;
          case ETAG:
            etag = node.getTextContent().replace("\"", "");
            break;
          case SIZE:
            size = getSize(node);
            break;
          case LAST_MODIFIED:
            lastModified = getUTCLastModified(node);
            break;
        }
      }
    } catch (InvalidArgumentException | MalformedURLException e) { // anything goes wrong...
      log.error("failed to parse DiscogsDump. reason: " + e.getMessage());
    }

    // but won't actually occur.
    if (lastModified == null) {
      return null;
    }

    return DiscogsDump.builder()
        .uriString(uri)
        .url(url)
        .size(size)
        .registeredAt(lastModified)
        .eTag(etag)
        .type(type)
        .createdAt(createdAt)
        .build();
  }

  private LocalDate parseCreatedAt(String content) {
    String[] parts = content.split("_");
    if (parts.length < 2) {
      return null;
    }
    String createdAtString = parts[1];
    int year, month, day;
    year = Integer.parseInt(createdAtString, 0, 4, 10);
    month = Integer.parseInt(createdAtString, 4, 6, 10);
    day = Integer.parseInt(createdAtString, 6, 8, 10);
    return LocalDate.of(year, month, day);
  }

  /**
   * Transform the text content of a node into a long value.
   *
   * @param node target node.
   * @return parsed value
   * @throws InvalidArgumentException thrown if {@link NumberFormatException} thrown.
   */
  protected Long getSize(Node node) throws InvalidArgumentException {
    String sizeString = node.getTextContent();
    try {
      return Long.parseLong(sizeString);
    } catch (NumberFormatException e) {
      throw new InvalidArgumentException("failed to parse [" + sizeString + "] into long value");
    }
  }

  /**
   * Transform the text content of the node into a {@link LocalDateTime} formatted in UTC timezone.
   *
   * @param node target node.
   * @return parsed LocalDateTime instance with UTC timezone.
   * @throws InvalidArgumentException thrown if {@link DateTimeParseException} thrown.
   */
  protected LocalDateTime getUTCLastModified(Node node) throws InvalidArgumentException {

    String target = node.getTextContent();

    if (target == null || target.isEmpty()) {
      throw new InvalidArgumentException(
          "cannot parse null or blank string into LocalDateTime instance");
    }

    try {
      return OffsetDateTime.parse(target).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    } catch (DateTimeParseException e) {
      throw new InvalidArgumentException(
          "failed to parse " + node.getTextContent() + " to OffsetDateTime");
    }
  }

  /**
   * Get {@link DumpType} of given entry. If we failed to parse the exact match, it will throw an
   * {@link InvalidArgumentException}.
   *
   * @param node target node.
   * @return parsed {@link DumpType} value.
   * @throws InvalidArgumentException thrown if we failed to recognize the type that node indicates.
   */
  protected DumpType getType(Node node) throws InvalidArgumentException {
    String content = node.getTextContent();
    if (ARTIST.matcher(content).matches()) {
      return DumpType.ARTIST;
    } else if (RELEASE_ITEM.matcher(content).matches()) {
      return DumpType.RELEASE;
    } else if (MASTER.matcher(content).matches()) {
      return DumpType.MASTER;
    } else if (LABEL.matcher(content).matches()) {
      return DumpType.LABEL;
    } else {
      throw new InvalidArgumentException("unknown dump type found for node content: " + content);
    }
  }

  /**
   * If given node has name that matches to one of the following: {KEY, LAST_MODIFIED, ETAG, SIZE}.
   *
   * @param node target node.
   * @return is one of the given list.
   */
  protected boolean isKnownNodeType(Node node) {
    return KNOWN_NODE_TYPES.contains(node.getNodeName());
  }

  /**
   * Check if node list is the GZip entry.
   *
   * @param nodeList node list to evaluate
   * @return true if it is GZip entry, else returns false.
   */
  protected boolean isXmlGzipEntry(NodeList nodeList) {
    for (int i = 0; i < nodeList.getLength(); i++) {
      if (XML_GZ_PATTERN.matcher(nodeList.item(i).getTextContent()).matches()) {
        return true;
      }
    }
    return false;
  }
}
