package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.common.exception.InvalidArgumentException;

import java.util.Locale;

public enum DumpType {
  ARTIST,
  RELEASE,
  LABEL,
  MASTER;

  @Override
  public String toString() {
    return this.name().toLowerCase(Locale.US);
  }

  public static DumpType of(String name) {
    String targetName = name.toLowerCase(Locale.US);
    for (DumpType value : values()) {
      if (value.toString().equals(targetName)) {
        return value;
      }
    }
    throw new InvalidArgumentException("failed to figure out type: " + name);
  }
}
