package io.dsub.discogs.batch.domain.master;

import java.time.LocalDateTime;
import java.util.Objects;

/** The desired master mapping observed for one Release root. */
public record MasterMainReleaseAssignment(
    int releaseId, Integer targetMasterId, LocalDateTime observedAt) {

  public MasterMainReleaseAssignment {
    observedAt = Objects.requireNonNull(observedAt, "observedAt");
  }
}
