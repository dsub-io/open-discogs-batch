package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.ImportExecutionException;
import io.dsub.opendiscogs.model.manifest.ImportExecution;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/** Verifies that partial imports reference compatible completed dependency snapshots. */
final class ImportDependencyPreflight {

  void validate(Connection connection, List<PlannedDump> dumps)
      throws SQLException, ImportExecutionException {
    List<Requirement> requirements = requirements(dumps);
    if (requirements.isEmpty()) {
      return;
    }
    try (PreparedStatement statement =
        connection.prepareStatement(ImportExecutionQueries.FIND_DEPENDENCY_CHECKPOINT)) {
      for (Requirement requirement : requirements) {
        validate(statement, requirement);
      }
    }
  }

  List<Requirement> requirements(List<PlannedDump> dumps) {
    EnumSet<EntityType> selected = EnumSet.noneOf(EntityType.class);
    for (PlannedDump dump : dumps) {
      selected.add(dump.entityType());
    }

    EnumMap<EntityType, RequirementAccumulator> requirements =
        new EnumMap<>(EntityType.class);
    for (PlannedDump dump : dumps) {
      LocalDate horizon = dump.dumpDate().withDayOfMonth(1).plusMonths(1);
      for (String value :
          ImportExecution.requiredLockEntityTypes(List.of(dump.entityType().toString()))) {
        EntityType dependency = EntityType.valueOf(value.toUpperCase(Locale.ROOT));
        if (dependency == dump.entityType() || selected.contains(dependency)) {
          continue;
        }
        RequirementAccumulator requirement =
            requirements.computeIfAbsent(
                dependency, ignored -> new RequirementAccumulator(dependency));
        requirement.require(dump.entityType(), horizon);
      }
    }
    return requirements.values().stream().map(RequirementAccumulator::toRequirement).toList();
  }

  private void validate(PreparedStatement statement, Requirement requirement)
      throws SQLException, ImportExecutionException {
    statement.setString(1, requirement.entityType().toString());
    statement.setInt(
        2, ImportExecution.importContractRevision(requirement.entityType().toString()));
    statement.setString(3, requirement.entityType().toString());
    statement.setDate(4, Date.valueOf(requirement.horizonExclusive()));
    try (ResultSet result = statement.executeQuery()) {
      if (!result.next()) {
        throw new ImportExecutionException(
            "partial import requires a completed "
                + requirement.entityType()
                + " checkpoint for "
                + requirement.requiredByText());
      }

      Date expectedDateValue = result.getDate("expected_date");
      validate(
          requirement,
          new Snapshot(
              result.getDate("checkpoint_date").toLocalDate(),
              result.getString("checkpoint_checksum"),
              expectedDateValue == null ? null : expectedDateValue.toLocalDate(),
              result.getString("expected_checksum")));
    }
  }

  void validate(Requirement requirement, Snapshot snapshot)
      throws ImportExecutionException {
    if (!snapshot.checkpointDate().isBefore(requirement.horizonExclusive())) {
      return;
    }
    if (snapshot.expectedDate() == null || snapshot.expectedChecksum() == null) {
      throw new ImportExecutionException(
          requirement.entityType()
              + " checkpoint "
              + snapshot.checkpointDate()
              + " has no immutable catalog provenance before "
              + requirement.horizonExclusive());
    }

    boolean staleDate = snapshot.checkpointDate().isBefore(snapshot.expectedDate());
    boolean reissued =
        snapshot.checkpointDate().equals(snapshot.expectedDate())
            && !normalizedChecksum(snapshot.checkpointChecksum())
                .equals(normalizedChecksum(snapshot.expectedChecksum()));
    if (staleDate || reissued) {
      throw new ImportExecutionException(
          requirement.entityType()
              + " checkpoint "
              + snapshot.checkpointDate()
              + " is stale for "
              + requirement.requiredByText()
              + "; latest compatible catalog dump is "
              + snapshot.expectedDate());
    }
  }

  private String normalizedChecksum(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  record PlannedDump(EntityType entityType, LocalDate dumpDate) {
    PlannedDump {
      if (entityType == null || dumpDate == null) {
        throw new IllegalArgumentException("dependency preflight dump fields must not be null");
      }
    }
  }

  record Requirement(
      EntityType entityType, List<EntityType> requiredBy, LocalDate horizonExclusive) {

    Requirement {
      requiredBy = List.copyOf(requiredBy);
    }

    String requiredByText() {
      return requiredBy.stream()
          .map(EntityType::toString)
          .sorted()
          .reduce((left, right) -> left + "," + right)
          .orElseThrow();
    }
  }

  record Snapshot(
      LocalDate checkpointDate,
      String checkpointChecksum,
      LocalDate expectedDate,
      String expectedChecksum) {
  }

  private static final class RequirementAccumulator {

    private final EntityType entityType;
    private final List<EntityType> requiredBy = new ArrayList<>();
    private LocalDate horizonExclusive = LocalDate.MIN;

    private RequirementAccumulator(EntityType entityType) {
      this.entityType = entityType;
    }

    private void require(EntityType requiringEntity, LocalDate horizon) {
      if (!requiredBy.contains(requiringEntity)) {
        requiredBy.add(requiringEntity);
      }
      if (horizon.isAfter(horizonExclusive)) {
        horizonExclusive = horizon;
      }
    }

    private Requirement toRequirement() {
      return new Requirement(entityType, requiredBy, horizonExclusive);
    }
  }
}
