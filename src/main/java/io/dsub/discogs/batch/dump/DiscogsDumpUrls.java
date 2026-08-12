package io.dsub.discogs.batch.dump;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Constructs canonical catalog, manifest, and dump locations. */
public final class DiscogsDumpUrls {

  public static final URI PUBLIC_CATALOG_URI = URI.create("https://data.discogs.com/");

  private static final String DOWNLOAD_PARAMETER = "download=";
  private static final String PREFIX_PARAMETER = "prefix=";
  private static final String DATA_PATH = "data/";
  private static final DateTimeFormatter DUMP_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

  private DiscogsDumpUrls() {
  }

  public static URI normalizeCatalogUri(URI catalogUri) {
    if (catalogUri == null
        || catalogUri.getHost() == null
        || (!"http".equalsIgnoreCase(catalogUri.getScheme())
            && !"https".equalsIgnoreCase(catalogUri.getScheme()))) {
      throw new IllegalArgumentException("Discogs data URI must use HTTP or HTTPS");
    }
    String value = catalogUri.toString();
    return URI.create(value.endsWith("/") ? value : value + "/");
  }

  public static URL catalogYear(URI catalogUri, int year) {
    String prefix = DATA_PATH + year + "/";
    return toUrl(
        URI.create(
            normalizeCatalogUri(catalogUri)
                + "?"
                + PREFIX_PARAMETER
                + URLEncoder.encode(prefix, StandardCharsets.UTF_8)));
  }

  public static URL manifest(URI catalogUri, LocalDate dumpDate) {
    return catalogDownload(catalogUri, manifestPath(dumpDate));
  }

  public static URL dump(URI catalogUri, String path) {
    return catalogDownload(catalogUri, path);
  }

  public static String manifestPath(LocalDate dumpDate) {
    return DATA_PATH
        + dumpDate.getYear()
        + "/discogs_"
        + DUMP_DATE_FORMATTER.format(dumpDate)
        + "_CHECKSUM.txt";
  }

  private static URL catalogDownload(URI catalogUri, String path) {
    return toUrl(
        URI.create(
            normalizeCatalogUri(catalogUri)
                + "?"
                + DOWNLOAD_PARAMETER
                + URLEncoder.encode(path, StandardCharsets.UTF_8)));
  }

  static URL toUrl(URI uri) {
    try {
      return uri.toURL();
    } catch (MalformedURLException exception) {
      throw new IllegalArgumentException("URI cannot be represented as a URL: " + uri, exception);
    }
  }
}
