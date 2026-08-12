package io.dsub.discogs.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.dump.EntityType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportDependencyPreflightUnitTest {

  private static final LocalDate JULY = LocalDate.of(2026, 7, 1);
  private static final LocalDate AUGUST = LocalDate.of(2026, 8, 1);
  private static final String CHECKSUM = "a".repeat(64);

  private final ImportDependencyPreflight preflight = new ImportDependencyPreflight();

  @Test
  void derivesOnlyDependenciesMissingFromTheSelectedPlan() {
    assertThat(
            preflight.requirements(
                List.of(
                    dump(EntityType.ARTIST, JULY),
                    dump(EntityType.LABEL, JULY))))
        .isEmpty();

    assertThat(preflight.requirements(List.of(dump(EntityType.MASTER, JULY))))
        .containsExactly(
            new ImportDependencyPreflight.Requirement(
                EntityType.ARTIST, List.of(EntityType.MASTER), AUGUST));

    assertThat(
            preflight.requirements(
                List.of(
                    dump(EntityType.MASTER, JULY.minusMonths(1)),
                    dump(EntityType.MASTER, JULY),
                    dump(EntityType.LABEL, JULY),
                    dump(EntityType.RELEASE, JULY))))
        .containsExactly(
            new ImportDependencyPreflight.Requirement(
                EntityType.ARTIST,
                List.of(EntityType.MASTER, EntityType.RELEASE),
                AUGUST));

    assertThat(
            preflight.requirements(
                List.of(
                    dump(EntityType.ARTIST, JULY),
                    dump(EntityType.LABEL, JULY),
                    dump(EntityType.MASTER, JULY),
                    dump(EntityType.RELEASE, JULY))))
        .isEmpty();
  }

  @Test
  void plannedDumpRejectsMissingBoundaryFields() {
    assertThatThrownBy(() -> new ImportDependencyPreflight.PlannedDump(null, JULY))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ImportDependencyPreflight.PlannedDump(EntityType.ARTIST, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsCurrentEquivalentAndNewerCheckpoints() throws Exception {
    ImportDependencyPreflight.Requirement requirement = requirement();

    preflight.validate(
        requirement,
        new ImportDependencyPreflight.Snapshot(JULY, CHECKSUM, JULY, CHECKSUM.toUpperCase()));
    preflight.validate(
        requirement,
        new ImportDependencyPreflight.Snapshot(AUGUST, CHECKSUM, null, null));
    preflight.validate(
        requirement,
        new ImportDependencyPreflight.Snapshot(
            JULY, null, JULY.minusDays(1), "b".repeat(64)));
  }

  @Test
  void rejectsMissingStaleAndReissuedCatalogProvenance() {
    ImportDependencyPreflight.Requirement requirement = requirement();

    assertThatThrownBy(
            () ->
                preflight.validate(
                    requirement,
                    new ImportDependencyPreflight.Snapshot(JULY, CHECKSUM, null, CHECKSUM)))
        .hasMessageContaining("no immutable catalog provenance");
    assertThatThrownBy(
            () ->
                preflight.validate(
                    requirement,
                    new ImportDependencyPreflight.Snapshot(JULY, CHECKSUM, JULY, null)))
        .hasMessageContaining("no immutable catalog provenance");
    assertThatThrownBy(
            () ->
                preflight.validate(
                    requirement,
                    new ImportDependencyPreflight.Snapshot(
                        JULY.minusDays(1), CHECKSUM, JULY, CHECKSUM)))
        .hasMessageContaining("is stale for master");
    assertThatThrownBy(
            () ->
                preflight.validate(
                    requirement,
                    new ImportDependencyPreflight.Snapshot(
                        JULY, CHECKSUM, JULY, "b".repeat(64))))
        .hasMessageContaining("latest compatible catalog dump is 2026-07-01");
    assertThatThrownBy(
            () ->
                preflight.validate(
                    requirement,
                    new ImportDependencyPreflight.Snapshot(JULY, null, JULY, CHECKSUM)))
        .hasMessageContaining("latest compatible catalog dump is 2026-07-01");
  }

  @Test
  void requirementRendersSortedConsumersAndRejectsAnEmptyConsumerSet() {
    ImportDependencyPreflight.Requirement requirement =
        new ImportDependencyPreflight.Requirement(
            EntityType.ARTIST,
            List.of(EntityType.RELEASE, EntityType.MASTER),
            AUGUST);
    assertThat(requirement.requiredByText()).isEqualTo("master,release");
    assertThatThrownBy(
            () ->
                new ImportDependencyPreflight.Requirement(
                        EntityType.ARTIST, List.of(), AUGUST)
                    .requiredByText())
        .isInstanceOf(java.util.NoSuchElementException.class);
  }

  private ImportDependencyPreflight.PlannedDump dump(EntityType type, LocalDate date) {
    return new ImportDependencyPreflight.PlannedDump(type, date);
  }

  private ImportDependencyPreflight.Requirement requirement() {
    return new ImportDependencyPreflight.Requirement(
        EntityType.ARTIST, List.of(EntityType.MASTER), AUGUST);
  }
}
