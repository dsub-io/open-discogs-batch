package io.dsub.discogs.batch.dump;

import io.dsub.discogs.batch.exception.DumpNotFoundException;
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
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
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

  private static final String DISCOGS_DATA_URL = DiscogsDumpUrls.PUBLIC_CATALOG_URI.toString();
  private static final String LEGACY_BUCKET_URL =
      "https://discogs-data.s3-us-west-2.amazonaws.com";
  private static final String USER_AGENT =
      "Mozilla/5.0 (compatible; OpenDiscogsBatch/0.1;"
          + " +https://github.com/dsub-io/open-discogs-batch)";
  private static final String ACCEPT_HTML =
      "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
  private static final String ACCEPT_TEXT = "text/plain,*/*;q=0.8";
  private static final String ACCEPT_DUMP = "application/gzip,application/octet-stream,*/*;q=0.8";
  private static final String CONTENT_LENGTH_HEADER = "Content-Length";
  private static final String HEAD_METHOD = "HEAD";
  private static final int HTTP_OK = 200;
  private static final int HTTP_NOT_FOUND = 404;
  private static final long KIBIBYTE = 1024L;
  private static final long MEBIBYTE = KIBIBYTE * 1024L;
  private static final long GIBIBYTE = MEBIBYTE * 1024L;
  private static final long TEBIBYTE = GIBIBYTE * 1024L;

  private final String discogsDataUrl;
  private final URI discogsDataUri;
  private final HttpClient httpClient =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .cookieHandler(createCookieManager())
          .build();

  public DefaultDumpSupplier() {
    this(URI.create(DISCOGS_DATA_URL));
  }

  protected DefaultDumpSupplier(URI discogsDataUri) {
    this.discogsDataUri = DiscogsDumpUrls.normalizeCatalogUri(discogsDataUri);
    discogsDataUrl = this.discogsDataUri.toString();
  }

  private static CookieManager createCookieManager() {
    CookieManager cookieManager = new CookieManager();
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    return cookieManager;
  }

  /** Supplies one pinned latest dump for every entity type. */
  @Override
  public List<DiscogsDump> get() {
    return getLatest(EnumSet.allOf(EntityType.class));
  }

  @Override
  public List<DiscogsDump> get(File file) {
    return parseDumpList(file).stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public List<DiscogsDump> getLatest(Set<EntityType> entities) {
    Set<EntityType> required = requireEntities(entities);
    try {
      List<String> yearIndexUrls =
          parseYearIndexUrls(getDiscogsDataSource(discogsDataUrl)).stream()
              .sorted(Comparator.reverseOrder())
              .toList();
      if (yearIndexUrls.isEmpty()) {
        throw new DumpNotFoundException("Discogs dump catalog contains no years");
      }

      List<DiscogsDump> candidates = new ArrayList<>();
      for (String yearIndexUrl : yearIndexUrls) {
        candidates.addAll(parseHtmlDumpList(getDiscogsDataSource(yearIndexUrl)));
        if (containsEveryType(required, candidates)) {
          break;
        }
      }
      return pinMetadata(requireComplete(required, candidates, "latest"));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw catalogFailure("latest", exception);
    } catch (IOException exception) {
      throw catalogFailure("latest", exception);
    }
  }

  @Override
  public List<DiscogsDump> getMonth(Set<EntityType> entities, YearMonth month) {
    Set<EntityType> required = requireEntities(entities);
    if (month == null) {
      throw new InvalidArgumentException("dump month cannot be null");
    }
    try {
      return resolveCatalogSelection(
          DiscogsDumpUrls.catalogYear(discogsDataUri, month.getYear()).toString(),
          required,
          month,
          "month " + month);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw catalogFailure("month " + month, exception);
    } catch (IOException exception) {
      throw catalogFailure("month " + month, exception);
    }
  }

  private Set<EntityType> requireEntities(Set<EntityType> entities) {
    if (entities == null || entities.isEmpty()) {
      throw new InvalidArgumentException("entities cannot be empty");
    }
    return EnumSet.copyOf(entities);
  }

  private List<DiscogsDump> resolveCatalogSelection(
      String yearIndexUrl,
      Set<EntityType> entities,
      YearMonth month,
      String selection)
      throws IOException, InterruptedException {
    List<DiscogsDump> candidates = parseHtmlDumpList(getDiscogsDataSource(yearIndexUrl));
    candidates =
        candidates.stream()
            .filter(dump -> YearMonth.from(dump.getLastModifiedAt()).equals(month))
            .toList();
    return pinMetadata(requireComplete(entities, candidates, selection));
  }

  private boolean containsEveryType(Set<EntityType> entities, Collection<DiscogsDump> candidates) {
    EnumSet<EntityType> found = EnumSet.noneOf(EntityType.class);
    candidates.stream()
        .map(DiscogsDump::getType)
        .filter(entities::contains)
        .forEach(found::add);
    return found.containsAll(entities);
  }

  private List<DiscogsDump> requireComplete(
      Set<EntityType> entities, Collection<DiscogsDump> candidates, String selection) {
    Map<EntityType, DiscogsDump> latest = new EnumMap<>(EntityType.class);
    candidates.stream()
        .filter(dump -> entities.contains(dump.getType()))
        .forEach(
            dump ->
                latest.merge(
                    dump.getType(),
                    dump,
                    (left, right) -> left.compareTo(right) >= 0 ? left : right));
    if (!latest.keySet().containsAll(entities)) {
      Set<EntityType> missing = EnumSet.copyOf(entities);
      missing.removeAll(latest.keySet());
      throw new DumpNotFoundException(
          "Discogs dump catalog for " + selection + " is missing " + missing);
    }
    return entities.stream()
        .sorted(Comparator.comparingInt(Enum::ordinal))
        .map(latest::get)
        .toList();
  }

  private List<DiscogsDump> pinMetadata(List<DiscogsDump> dumps)
      throws IOException, InterruptedException {
    Map<String, Map<String, String>> manifests = new HashMap<>();
    List<String> checksums = new ArrayList<>(dumps.size());
    for (DiscogsDump dump : dumps) {
      URL manifestUrl = dump.getChecksumUrl();
      String manifestKey = manifestUrl.toString();
      Map<String, String> manifest = manifests.get(manifestKey);
      if (manifest == null) {
        String source =
            getDiscogsManifestSource(manifestKey)
                .orElseThrow(
                    () ->
                        new DumpNotFoundException(
                            "Discogs checksum manifest not found for "
                                + dump.getLastModifiedAt()));
        manifest = DiscogsDumpVerifier.parseChecksums(source);
        manifests.put(manifestKey, manifest);
      }
      String checksum = manifest.get(dump.getFileName());
      if (checksum == null) {
        throw new DumpNotFoundException(
            "Discogs checksum manifest is missing " + dump.getFileName());
      }
      checksums.add(checksum);
    }

    List<DiscogsDump> pinned = new ArrayList<>(dumps.size());
    for (int index = 0; index < dumps.size(); index++) {
      DiscogsDump dump = dumps.get(index);
      pinned.add(
          new DiscogsDump(
              dump.getETag(),
              dump.getType(),
              dump.getUriString(),
              getDiscogsDumpSize(dump.getUrl()),
              dump.getLastModifiedAt(),
              dump.getUrl(),
              dump.getChecksumUrl(),
              checksums.get(index)));
    }
    return List.copyOf(pinned);
  }

  private DumpNotFoundException catalogFailure(String selection, Exception exception) {
    return new DumpNotFoundException(
        "failed to refresh Discogs dump catalog for "
            + selection
            + ": "
            + exception.getMessage());
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
    if (response.statusCode() != HTTP_OK) {
      throw new IOException(
          "Discogs data index returned HTTP " + response.statusCode() + " for " + url);
    }
    return response.body();
  }

  protected Optional<String> getDiscogsManifestSource(String url)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", ACCEPT_TEXT)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();
    HttpResponse<String> response =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (response.statusCode() == HTTP_NOT_FOUND) {
      return Optional.empty();
    }
    if (response.statusCode() != HTTP_OK) {
      throw new IOException(
          "Discogs checksum manifest returned HTTP " + response.statusCode() + " for " + url);
    }
    return Optional.of(response.body());
  }

  protected long getDiscogsDumpSize(URL dumpUrl) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(dumpUrl.toString()))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", ACCEPT_DUMP)
            .header("User-Agent", USER_AGENT)
            .method(HEAD_METHOD, HttpRequest.BodyPublishers.noBody())
            .build();
    HttpResponse<Void> response =
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    if (response.statusCode() != HTTP_OK) {
      throw new IOException(
          "Discogs dump metadata returned HTTP " + response.statusCode() + " for " + dumpUrl);
    }
    OptionalLong contentLength = response.headers().firstValueAsLong(CONTENT_LENGTH_HEADER);
    if (contentLength.isEmpty() || contentLength.getAsLong() <= 0L) {
      throw new IOException(
          "Discogs dump metadata omitted a positive Content-Length for " + dumpUrl);
    }
    return contentLength.getAsLong();
  }

  protected List<String> parseYearIndexUrls(String html) {
    if (html == null || html.isBlank()) {
      return List.of();
    }
    Set<String> urls = new LinkedHashSet<>();
    Matcher matcher = YEAR_INDEX_LINK_PATTERN.matcher(html);
    while (matcher.find()) {
      urls.add(discogsDataUrl + "?prefix=data%2F" + matcher.group(1) + "%2F");
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
        if (!uri.endsWith("/" + fileName)) {
          continue;
        }
        if (!XML_GZ_PATTERN.matcher(uri).matches()) {
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
                URI.create(discogsDataUrl).resolve(matcher.group(3)).toURL(),
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
            URI.create(discogsDataUrl).resolve(matcher.group(1)).toURL());
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
      log.error("failed to parse Discogs dump catalog {}", file, e);
    }
    return List.of();
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
        if (content.isEmpty()) {
          return null;
        }
        String nodeName = node.getNodeName();
        if (KEY.equals(nodeName)) {
          uri = content; // formatted as 'data/{year}/{file_name}'
          url = URI.create(LEGACY_BUCKET_URL + "/" + uri).toURL();
          type = getType(content);
          lastModified = parseLastModifiedAt(uri);
        } else if (ETAG.equals(nodeName)) {
          etag = content.replace("\"", "");
        } else {
          size = getSize(node);
        }
      }
    } catch (InvalidArgumentException | IllegalArgumentException | MalformedURLException e) {
      log.error("failed to parse DiscogsDump. reason: " + e.getMessage());
      return null;
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
