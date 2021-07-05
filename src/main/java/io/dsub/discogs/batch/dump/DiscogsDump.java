package io.dsub.discogs.batch.dump;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DiscogsDump implements Comparable<DiscogsDump> {

  private final String eTag;
  private final EntityType type;
  private final String uriString;
  private final Long size;
  private final LocalDate lastModifiedAt;
  private final URL url;

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
   * Compare only equals with ETag value as it is the single most definite identification of a
   * dump.
   *
   * @param o any object, or another instance of dump to be evaluated being equal.
   * @return the result of the equals method.
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
