package io.dsub.discogs.batch.dump;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Slf4j
@Component
public class DefaultDumpSupplier implements DumpSupplier {

  private static final Pattern XML_GZ_PATTERN =
      Pattern.compile("^[\\w/_-]+\\.xml\\.gz$", Pattern.CASE_INSENSITIVE);
  private static final Pattern ARTIST =
      Pattern.compile(".*artists.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern RELEASE_ITEM =
      Pattern.compile(".*releases.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern MASTER =
      Pattern.compile(".*masters.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern LABEL =
      Pattern.compile(".*labels.*", Pattern.CASE_INSENSITIVE);
  private static final Pattern YEAR_INDEX_LINK_PATTERN =
      Pattern.compile(
          "href=[\"']\\?prefix=data%2[fF](\\d{4})%2[fF][\"']",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern HTML_DUMP_ENTRY_PATTERN =
      Pattern.compile(
          "(?m)^[ \\t]*\\d{4}-\\d{2}-\\d{2}[ \\t]+\\d{2}:\\d{2}:\\d{2}"
              + "[ \\t]+([\\d.]+)[ \\t]+(B|KB|MB|GB|TB)[ \\t]+"
              + "<a[ \\t]+href=[\"']((?:https?://data\\.discogs\\.com/)?"
              + "\\?download=([^\"']+))[\"'][^>]*>([^<]+\\.xml\\.gz)</a>[ \\t]*$",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern HTML_CHECKSUM_ENTRY_PATTERN =
      Pattern.compile(
          "<a[ \\t]+href=[\"']((?:https?://data\\.discogs\\.com/)?"
              + "\\?download=([^\"']+))[\"'][^>]*>"
              + "(discogs_(\\d{8})_CHECKSUM\\.txt)</a>",
          Pattern.CASE_INSENSITIVE);

  private static final String CONTENTS_TAG_NAME = "Contents";
  private static final String KEY = "Key";
  private static final String ETAG = "ETag";
  private static final String SIZE = "Size";

  private static final List<String> KNOWN_NODE_TYPES = List.of(KEY, ETAG, SIZE);

  private static final String DISCOGS_DATA_URL = "https://data.discogs.com/";
  private static final String LEGACY_BUCKET_URL =
      "https://discogs-data.s3-us-west-2.amazonaws.com";
  private static final String USER_AGENT =
      "Mozilla/5.0 (compatible; OpenDiscogsBatch/0.1;"
          + " +https://github.com/dsub-io/open-discogs-batch)";
  private static final String ACCEPT_HTML =
      "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
  private static final long KIBIBYTE = 1024L;
  private static final long MEBIBYTE = KIBIBYTE * 1024L;
  private static final long GIBIBYTE = MEBIBYTE * 1024L;
  private static final long TEBIBYTE = GIBIBYTE * 1024L;

  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .cookieHandler(createCookieManager())
          .build();

  private static CookieManager createCookieManager() {
    CookieManager cookieManager = new CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    return cookieManager;
  }

  /** Supplies every dump exposed by the year indexes at {@code data.discogs.com}. */
  @Override
  public List<DiscogsDump> get() {
    List<DiscogsDump> dumps = new ArrayList<>();
    try {
      String rootIndex = getDiscogsDataSource(DISCOGS_DATA_URL);
      for (String yearIndexUrl : parseYearIndexUrls(rootIndex)) {
        try {
          dumps.addAll(parseHtmlDumpList(getDiscogsDataSource(yearIndexUrl)));
        } catch (IOException e) {
          log.warn("failed to fetch Discogs data index {}", yearIndexUrl, e);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("interrupted while fetching the Discogs data index", e);
    } catch (IOException e) {
      log.error("failed to fetch the Discogs data index", e);
    }
    return dumps;
  }

  @Override
  public List<DiscogsDump> get(File file) {
    return parseDumpList(file).stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  protected String getDiscogsDataSource(String url) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", ACCEPT_HTML)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();
    HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() != 200) {
      throw new IOException(
          "Discogs data index returned HTTP " + response.statusCode() + " for " + url);
    }
    return response.body();
  }

  protected List<String> parseYearIndexUrls(String html) {
    if (html == null || html.isBlank()) {
      return List.of();
    }
    Set<String> urls = new LinkedHashSet<>();
    Matcher matcher = YEAR_INDEX_LINK_PATTERN.matcher(html);
    while (matcher.find()) {
      urls.add(DISCOGS_DATA_URL + "?prefix=data%2F" + matcher.group(1) + "%2F");
    }
    return new ArrayList<>(urls);
  }

  protected List<DiscogsDump> parseHtmlDumpList(String html) {
    if (html == null || html.isBlank()) {
      return List.of();
    }
    Map<LocalDate, URL> checksumUrls = parseChecksumUrls(html);
    List<DiscogsDump> dumps = new ArrayList<>();
    Matcher matcher = HTML_DUMP_ENTRY_PATTERN.matcher(html);
    while (matcher.find()) {
      String fileName = matcher.group(5);
      try {
        String encodedUri = matcher.group(4);
        String uri = URLDecoder.decode(encodedUri, StandardCharsets.UTF_8);
        if (!uri.endsWith("/" + fileName) || !XML_GZ_PATTERN.matcher(uri).matches()) {
          continue;
        }
        LocalDate dumpDate = parseLastModifiedAt(uri);
        URL checksumUrl = checksumUrls.get(dumpDate);
        if (checksumUrl == null) {
          log.warn("skipping Discogs dump without a matching checksum: {}", fileName);
          continue;
        }
        dumps.add(
            new DiscogsDump(
                uri,
                getType(uri),
                uri,
                parseDisplaySize(matcher.group(1), matcher.group(2)),
                dumpDate,
                URI.create(DISCOGS_DATA_URL).resolve(matcher.group(3)).toURL(),
                checksumUrl));
      } catch (InvalidArgumentException | IllegalArgumentException | MalformedURLException e) {
        log.warn("skipping malformed Discogs dump entry {}", fileName, e);
      }
    }
    return dumps;
  }

  protected Map<LocalDate, URL> parseChecksumUrls(String html) {
    if (html == null || html.isBlank()) {
      return Map.of();
    }
    Map<LocalDate, URL> checksumUrls = new HashMap<>();
    Matcher matcher = HTML_CHECKSUM_ENTRY_PATTERN.matcher(html);
    while (matcher.find()) {
      String fileName = matcher.group(3);
      try {
        String uri = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
        if (!uri.endsWith("/" + fileName)) {
          continue;
        }
        checksumUrls.put(
            parseLastModifiedAt(fileName),
            URI.create(DISCOGS_DATA_URL).resolve(matcher.group(1)).toURL());
      } catch (InvalidArgumentException | IllegalArgumentException | MalformedURLException e) {
        log.warn("skipping malformed Discogs checksum entry {}", fileName, e);
      }
    }
    return checksumUrls;
  }

  protected long parseDisplaySize(String value, String unit) {
    long multiplier =
        switch (unit.toUpperCase(Locale.ROOT)) {
          case "B" -> 1L;
          case "KB" -> KIBIBYTE;
          case "MB" -> MEBIBYTE;
          case "GB" -> GIBIBYTE;
          case "TB" -> TEBIBYTE;
          default -> throw new InvalidArgumentException("unknown file size unit: " + unit);
        };
    try {
      return new BigDecimal(value)
          .multiply(BigDecimal.valueOf(multiplier))
          .setScale(0, RoundingMode.HALF_UP)
          .longValueExact();
    } catch (ArithmeticException | NumberFormatException e) {
      throw new InvalidArgumentException("failed to parse file size: " + value + " " + unit);
    }
  }

  protected List<DiscogsDump> parseDumpList(File file) {
    if (file == null) {
      return new ArrayList<>();
    }
    try (InputStream in = new FileInputStream(file)) {
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
    } catch (SAXException | ParserConfigurationException | IOException e) {
      e.printStackTrace();
    }
    return null;
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
    EntityType type = null;
    String uri = null;
    LocalDate lastModified = null;
    String etag = null;
    Long size = null;
    URL url = null;

    try {
      // loop through the target nodes.
      for (Node node : targetNodes) {
        String content = node.getTextContent();
        if (content == null || content.isEmpty()) {
          return null;
        }
        switch (node.getNodeName()) {
          case KEY -> {
            uri = content; // formatted as 'data/{year}/{file_name}'
            url = URI.create(LEGACY_BUCKET_URL + "/" + uri).toURL();
            type = getType(content);
            lastModified = parseLastModifiedAt(uri);
          }
          case ETAG -> etag = node.getTextContent().replace("\"", "");
          case SIZE -> size = getSize(node);
        }
      }
    } catch (InvalidArgumentException | IllegalArgumentException | MalformedURLException e) {
      log.error("failed to parse DiscogsDump. reason: " + e.getMessage());
    }
    return new DiscogsDump(etag, type, uri, size, lastModified, url);
  }

  protected LocalDate parseLastModifiedAt(String content) {
    String[] parts = content.split("_");
    if (parts.length < 2) {
      throw new InvalidArgumentException("dump path does not contain a date: " + content);
    }
    String createdAtString = parts[1];
    try {
      int year = Integer.parseInt(createdAtString, 0, 4, 10);
      int month = Integer.parseInt(createdAtString, 4, 6, 10);
      int day = Integer.parseInt(createdAtString, 6, 8, 10);
      return LocalDate.of(year, month, day);
    } catch (DateTimeException | IndexOutOfBoundsException | NumberFormatException e) {
      throw new InvalidArgumentException("invalid dump date in path: " + content);
    }
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
   * Get {@link EntityType} of given entry. If we failed to parse the exact match, it will throw an
   * {@link InvalidArgumentException}.
   *
   * @param node target node.
   * @return parsed {@link EntityType} value.
   * @throws InvalidArgumentException thrown if we failed to recognize the type that node
   *                                  indicates.
   */
  protected EntityType getType(Node node) throws InvalidArgumentException {
    return getType(node.getTextContent());
  }

  protected EntityType getType(String content) throws InvalidArgumentException {
    if (ARTIST.matcher(content).matches()) {
      return EntityType.ARTIST;
    } else if (RELEASE_ITEM.matcher(content).matches()) {
      return EntityType.RELEASE;
    } else if (MASTER.matcher(content).matches()) {
      return EntityType.MASTER;
    } else if (LABEL.matcher(content).matches()) {
      return EntityType.LABEL;
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
