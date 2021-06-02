package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.common.exception.InitializationFailureException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "discogs_dump")
public class DiscogsDump implements Comparable<DiscogsDump>, Cloneable {

  @Id
  @Column(name = "etag")
  private String eTag;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private DumpType type;

  @Column(name = "uri_string")
  private String uriString;

  @Column(name = "size")
  private Long size;

  // date when registered to aws
  @Column(name = "registered_at")
  private LocalDateTime registeredAt;

  // date when we persisted to db...
  @Column(name = "created_at")
  private LocalDate createdAt;

  @Column(name = "url")
  private URL url;

  @Transient
  private Path resourcePath;

  // parse file name from the uriString formatted as data/{year}/{file_name};
  public String getFileName() {
    if (this.uriString == null || this.uriString.isBlank()) {
      return null;
    }
    return this.uriString.substring(this.uriString.lastIndexOf('/') + 1);
  }

  public Path getResourcePath() {
    try {
      if (this.resourcePath == null) {
        this.resourcePath = Files.createTempFile(null, getFileName());
        this.resourcePath.toFile().deleteOnExit(); // as last resort to delete if ...
      }
      return this.resourcePath;
    } catch (IOException e) {
      throw new InitializationFailureException("failed to create temporary file: " + resourcePath);
    }
  }

  @Override
  public int compareTo(DiscogsDump that) {
    return this.createdAt.compareTo(that.createdAt);
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

  public DiscogsDump getClone() {
    try {
      return (DiscogsDump) this.clone();
    } catch (CloneNotSupportedException ignored) {
      return null;
    }
  }
}