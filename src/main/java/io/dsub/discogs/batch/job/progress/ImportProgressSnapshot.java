package io.dsub.discogs.batch.job.progress;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

public record ImportProgressSnapshot(
    long committedItems,
    OptionalLong totalItems,
    Optional<Instant> lastCommittedProgressAt) {
}
