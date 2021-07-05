package io.dsub.discogs.batch.dump;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.List;
import java.util.Locale;

public enum EntityType {
  ARTIST,
  LABEL,
  MASTER,
  RELEASE;

  public static EntityType of(String name) throws InvalidArgumentException {
    String targetName = name.toLowerCase(Locale.US);
    for (EntityType value : values()) {
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

  public List<EntityType> getDependencies() {
    if (this.equals(RELEASE)) {
      return List.of(values());
    }
    if (this.equals(MASTER)) {
      return List.of(ARTIST, LABEL, MASTER);
    }
    return List.of(this);
  }
}
