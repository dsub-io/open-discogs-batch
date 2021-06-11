package io.dsub.discogs.batch.dump;

import io.dsub.discogs.common.exception.InvalidArgumentException;
import java.util.List;
import java.util.Locale;

public enum DumpType {
  ARTIST,
  LABEL,
  MASTER,
  RELEASE;

  public static DumpType of(String name) {
    String targetName = name.toLowerCase(Locale.US);
    for (DumpType value : values()) {
      if (value.toString().equals(targetName)) {
        return value;
      }
    }
    throw new InvalidArgumentException("failed to figure out type: " + name);
  }

  @Override
  public String toString() {
    return this.name().toLowerCase(Locale.US);
  }

  public List<DumpType> getDependencies() {
    if (this.equals(RELEASE)) {
      return List.of(values());
    }
    if (this.equals(MASTER)) {
      return List.of(ARTIST, LABEL, MASTER);
    }
    return List.of(this);
  }
}
