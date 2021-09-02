package io.dsub.discogs.batch.job.registry;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Entity ID cache to reduce DB lookups for entry.
 * The behaviors are up to its implementation.
 */
public interface EntityIdRegistry {

  boolean exists(Type type, Integer id);

  boolean exists(Type type, String id);

  void put(Type type, Integer id);

  void put(Type type, String id);

  void invert(Type type);

  void clearAll();

  ConcurrentSkipListSet<String> getStringIdSetByType(Type type);

  IdCache getLongIdCache(Type type);

  enum Type {
    ARTIST, LABEL, MASTER, RELEASE, GENRE, STYLE
  }
}
