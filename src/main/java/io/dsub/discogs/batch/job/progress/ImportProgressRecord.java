package io.dsub.discogs.batch.job.progress;

import io.dsub.discogs.batch.dump.EntityType;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;

public record ImportProgressRecord(
    ImportProgressState state,
    EntityType entityType,
    long committedItems,
    OptionalDouble committedPercent,
    double rowsPerSecond,
    Duration elapsed,
    boolean resumed,
    long initialCommittedItems,
    Optional<Instant> lastCommittedProgressAt,
    Optional<String> observationError) {
}
