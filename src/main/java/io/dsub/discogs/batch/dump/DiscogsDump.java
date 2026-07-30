package io.dsub.discogs.batch.dump;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Data;

@Data
public class DiscogsDump implements Comparable<DiscogsDump> {

  private final String eTag;
  private final EntityType type;
  private final String uriString;
  private final Long size;
  private final LocalDate lastModifiedAt;
  private final URL url;
  private final URL checksumUrl;

  public DiscogsDump(
      String eTag,
      EntityType type,
      String uriString,
      Long size,
      LocalDate lastModifiedAt,
      URL url) {
    this(eTag, type, uriString, size, lastModifiedAt, url, null);
  }

  public DiscogsDump(
      String eTag,
      EntityType type,
      String uriString,
      Long size,
      LocalDate lastModifiedAt,
      URL url,
      URL checksumUrl) {
    this.eTag = eTag;
    this.type = type;
    this.uriString = uriString;
    this.size = size;
    this.lastModifiedAt = lastModifiedAt;
    this.url = url;
    this.checksumUrl = checksumUrl;
  }

  public InputStream getInputStream() throws IOException {
    if (this.url == null) {
      return InputStream.nullInputStream();
    }
    return this.url.openStream();
  }

  // parse file name from the uriString formatted as data/{year}/{file_name};
  public String getFileName() {
    if (this.uriString == null || this.uriString.isBlank()) {
      return null;
    }
    return this.uriString.substring(this.uriString.lastIndexOf('/') + 1);
  }

  @Override
  public int compareTo(DiscogsDump that) {
    int res = this.lastModifiedAt.compareTo(that.lastModifiedAt);
    if (res != 0) {
      return res;
    }
    res = this.type.compareTo(that.getType());
    if (res != 0) {
      return res;
    }
    res = this.eTag.compareTo(that.getETag());
    if (res != 0) {
      return res;
    }
    return this.size.compareTo(that.getSize());
  }

  /**
   * Compares the stable dump identifier stored in the legacy {@code eTag} field. Current HTML
   * indexes no longer expose an object ETag, so their versioned dump path is used instead.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DiscogsDump that = (DiscogsDump) o;
    return eTag.equals(that.eTag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eTag);
  }
}
