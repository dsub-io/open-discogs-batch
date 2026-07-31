package io.dsub.discogs.batch.dump;

import io.dsub.discogs.batch.exception.FileException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DiscogsDumpVerifier {

  private static final Pattern CHECKSUM_LINE_PATTERN =
      Pattern.compile(
          "(?m)^([a-f\\d]{64})[ \\t]+\\*?(?:.*/)?([^/\\r\\n]+\\.xml\\.gz)[ \\t]*$",
          Pattern.CASE_INSENSITIVE);
  private static final String USER_AGENT = "open-discogs-batch/0.1";

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final Map<URI, Map<String, String>> checksumCache = new ConcurrentHashMap<>();

  /**
   * Verifies a downloaded dump against Discogs' SHA-256 manifest. Legacy dump records without a
   * checksum URL fall back to their exact object size.
   */
  public boolean isValid(DiscogsDump dump, Path file) throws FileException {
    if (dump.getChecksumUrl() == null) {
      return matchesLegacySize(dump, file);
    }

    String expected = getChecksums(dump.getChecksumUrl()).get(dump.getFileName());
    if (expected == null) {
      throw new FileException(
          "checksum for " + dump.getFileName() + " is missing from " + dump.getChecksumUrl());
    }
    return expected.equalsIgnoreCase(calculateSha256(file));
  }

  /**
   * Returns the normalized SHA-256 value that identifies a dump before it is downloaded.
   */
  public String getExpectedChecksum(DiscogsDump dump) throws FileException {
    if (dump.getChecksumUrl() == null) {
      throw new FileException(
          "SHA-256 manifest is required for idempotent import: " + dump.getFileName());
    }
    String expected = getChecksums(dump.getChecksumUrl()).get(dump.getFileName());
    if (expected == null) {
      throw new FileException(
          "checksum for " + dump.getFileName() + " is missing from " + dump.getChecksumUrl());
    }
    return expected.toLowerCase(Locale.ROOT);
  }

  protected synchronized Map<String, String> getChecksums(URL checksumUrl) throws FileException {
    URI checksumUri;
    try {
      checksumUri = checksumUrl.toURI();
    } catch (Exception e) {
      throw new FileException("invalid checksum URL: " + checksumUrl, e);
    }

    Map<String, String> cached = checksumCache.get(checksumUri);
    if (cached != null) {
      return cached;
    }

    Map<String, String> parsed = parseChecksums(getChecksumSource(checksumUri));
    if (parsed.isEmpty()) {
      throw new FileException("no SHA-256 entries found in " + checksumUrl);
    }
    Map<String, String> immutable = Collections.unmodifiableMap(parsed);
    Map<String, String> existing = checksumCache.putIfAbsent(checksumUri, immutable);
    return existing == null ? immutable : existing;
  }

  protected String getChecksumSource(URI checksumUri) throws FileException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(checksumUri)
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "text/plain")
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
        throw new FileException(
            "checksum returned HTTP " + response.statusCode() + " for " + checksumUri);
      }
      return response.body();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new FileException("interrupted while fetching checksum " + checksumUri, e);
    } catch (IOException e) {
      throw new FileException("failed to fetch checksum " + checksumUri, e);
    }
  }

  static Map<String, String> parseChecksums(String source) {
    if (source == null || source.isBlank()) {
      return Map.of();
    }
    Map<String, String> checksums = new HashMap<>();
    Matcher matcher = CHECKSUM_LINE_PATTERN.matcher(source);
    while (matcher.find()) {
      checksums.put(matcher.group(2), matcher.group(1).toLowerCase(Locale.ROOT));
    }
    return checksums;
  }

  protected String calculateSha256(Path file) throws FileException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (var input = Files.newInputStream(file)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
          digest.update(buffer, 0, read);
        }
      }
      return toHex(digest.digest());
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new FileException("failed to calculate SHA-256 for " + file, e);
    }
  }

  private boolean matchesLegacySize(DiscogsDump dump, Path file) throws FileException {
    try {
      return dump.getSize() != null && Files.size(file) == dump.getSize();
    } catch (IOException e) {
      throw new FileException("failed to fetch size from " + file, e);
    }
  }

  private String toHex(byte[] bytes) {
    StringBuilder value = new StringBuilder(bytes.length * 2);
    for (byte item : bytes) {
      value.append(String.format("%02x", item));
    }
    return value.toString();
  }
}
