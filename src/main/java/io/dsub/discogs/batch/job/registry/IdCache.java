package io.dsub.discogs.batch.job.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLongArray;
import lombok.Getter;

/**
 * Concurrent, segmented bit set for positive Discogs identifiers.
 *
 * <p>A segment is allocated only when an identifier in that 65,536-value range is observed. Dense
 * identifiers therefore require one bit each instead of one boxed {@link Integer} and one skip-list
 * node each. Segment words use compare-and-set so chunk workers can register identifiers without a
 * global lock.
 */
public class IdCache {

  private static final int SEGMENT_SHIFT = 16;
  private static final int SEGMENT_MASK = (1 << SEGMENT_SHIFT) - 1;
  private static final int WORD_SHIFT = 6;
  private static final int WORDS_PER_SEGMENT = 1 << (SEGMENT_SHIFT - WORD_SHIFT);

  @Getter private final DefaultEntityIdRegistry.Type type;
  private final ConcurrentHashMap<Integer, AtomicLongArray> segments = new ConcurrentHashMap<>();

  public IdCache(DefaultEntityIdRegistry.Type type) {
    this.type = type;
  }

  public boolean exists(Integer item) {
    if (item == null || item < 1) {
      return false;
    }
    AtomicLongArray segment = segments.get(item >>> SEGMENT_SHIFT);
    if (segment == null) {
      return false;
    }
    int segmentOffset = item & SEGMENT_MASK;
    int wordIndex = segmentOffset >>> WORD_SHIFT;
    long mask = 1L << (segmentOffset & 63);
    return (segment.get(wordIndex) & mask) != 0;
  }

  public void add(Integer item) {
    if (item == null || item < 1) {
      return;
    }
    AtomicLongArray segment =
        segments.computeIfAbsent(
            item >>> SEGMENT_SHIFT, ignored -> new AtomicLongArray(WORDS_PER_SEGMENT));
    int segmentOffset = item & SEGMENT_MASK;
    int wordIndex = segmentOffset >>> WORD_SHIFT;
    long mask = 1L << (segmentOffset & 63);
    segment.getAndUpdate(wordIndex, current -> current | mask);
  }

  public boolean isEmpty() {
    return segments.isEmpty();
  }

  public void clear() {
    segments.clear();
  }

  long allocatedWordBytes() {
    return (long) segments.size() * WORDS_PER_SEGMENT * Long.BYTES;
  }
}
