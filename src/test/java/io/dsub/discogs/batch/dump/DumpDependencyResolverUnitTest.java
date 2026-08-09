package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.TestArguments;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.DefaultApplicationArguments;

class DumpDependencyResolverUnitTest {

  @Mock private DiscogsDumpService dumpService;

  private DefaultDumpDependencyResolver resolver;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    resolver = new DefaultDumpDependencyResolver(dumpService);
  }

  @Test
  void noEntitiesOrMonthSelectsLatestDumpIndependentlyForEveryEntity() {
    for (EntityType type : EntityType.values()) {
      when(dumpService.getMostRecentDiscogsDumpByType(type))
          .thenReturn(TestArguments.getRandomDumpWithType(type, LocalDate.of(2026, type.ordinal() + 1, 1)));
    }

    Collection<DiscogsDump> result = resolver.resolve(new DefaultApplicationArguments());

    assertThat(result).extracting(DiscogsDump::getType).containsExactlyElementsOf(List.of(EntityType.values()));
    assertThat(result).extracting(DiscogsDump::getLastModifiedAt).doesNotHaveDuplicates();
  }

  @Test
  void entitiesSelectOnlyTheRequestedLatestDumpsWithoutAddingDependencies() {
    DiscogsDump release = TestArguments.getRandomDumpWithType(EntityType.RELEASE);
    when(dumpService.getMostRecentDiscogsDumpByType(EntityType.RELEASE)).thenReturn(release);

    Collection<DiscogsDump> result =
        resolver.resolve(new DefaultApplicationArguments("--entities=release"));

    assertThat(result).containsExactly(release);
    verify(dumpService, never()).getMostRecentDiscogsDumpByType(EntityType.ARTIST);
    verify(dumpService, never()).getMostRecentDiscogsDumpByType(EntityType.LABEL);
    verify(dumpService, never()).getMostRecentDiscogsDumpByType(EntityType.MASTER);
  }

  @Test
  void explicitDumpMonthRequiresThatExactMonth() {
    List<DiscogsDump> expected = List.of(TestArguments.getRandomDumpWithType(EntityType.ARTIST));
    when(dumpService.getAllByTypeYearMonth(List.of(EntityType.ARTIST), 2026, 7))
        .thenReturn(expected);

    Collection<DiscogsDump> result =
        resolver.resolve(
            new DefaultApplicationArguments("--entities=artist", "--dumpMonth=2026-07"));

    assertThat(result).isEqualTo(expected);
    verify(dumpService).getAllByTypeYearMonth(List.of(EntityType.ARTIST), 2026, 7);
  }

  @Test
  void duplicateEntitiesAreCollapsed() {
    Set<EntityType> result =
        resolver.parseEntities(
            new DefaultApplicationArguments("--entities=artist", "--entities=artist"));

    assertThat(result).containsExactly(EntityType.ARTIST);
  }

  @Test
  void missingLatestEntityDumpFailsInsteadOfSilentlyRollingBack() {
    when(dumpService.getMostRecentDiscogsDumpByType(EntityType.MASTER)).thenReturn(null);

    assertThatThrownBy(
            () -> resolver.resolve(new DefaultApplicationArguments("--entities=master")))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessage("failed to locate latest dump for master");
  }
}
