package io.dsub.discogs.batch.dump.service;

import static io.dsub.discogs.batch.TestArguments.getRandomDump;
import static io.dsub.discogs.batch.TestArguments.getRandomDumpWithType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.TestArguments;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.repository.DiscogsDumpRepository;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InitializationFailureException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DefaultDiscogsDumpServiceUnitTest {

  final Random random = new Random();
  @RegisterExtension
  public LogSpy logSpy = new LogSpy();
  @Mock
  DiscogsDumpRepository repository;
  @Mock
  DumpSupplier dumpSupplier;
  @InjectMocks
  DefaultDiscogsDumpService dumpService;
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void updateDbRefreshesAndPersistsOneLatestSelection() {
    Set<EntityType> types = Set.of(EntityType.values());
    List<DiscogsDump> expected =
        List.of(EntityType.values()).stream()
            .map(TestArguments::getRandomDumpWithType)
            .toList();
    when(dumpSupplier.getLatest(types)).thenReturn(expected);
    expected.forEach(dump -> when(repository.findTopByType(dump.getType())).thenReturn(dump));

    dumpService.updateDB();

    verify(dumpSupplier).getLatest(types);
    verify(repository).saveAll(expected);
  }

  @Test
  void latestRefreshFailureFallsBackToTheDurableCatalog() {
    Set<EntityType> types = Set.of(EntityType.ARTIST);
    DiscogsDump cached = getRandomDumpWithType(EntityType.ARTIST);
    when(dumpSupplier.getLatest(types)).thenThrow(new DumpNotFoundException("HTTP 429"));
    when(repository.findTopByType(EntityType.ARTIST)).thenReturn(cached);

    assertThat(dumpService.resolveLatest(types)).isEqualTo(List.of(cached));
    verify(repository, never()).saveAll(any());
  }

  @Test
  void latestRefreshFailureWithoutDurableCatalogPreservesTheReason() {
    Set<EntityType> types = Set.of(EntityType.RELEASE);
    when(dumpSupplier.getLatest(types)).thenThrow(new DumpNotFoundException("HTTP 429"));

    assertThat(catchThrowable(() -> dumpService.resolveLatest(types)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("HTTP 429")
        .hasMessageContaining("durable dump catalog");
  }

  @Test
  void emptyLatestRefreshUsesTheSameDurableFallback() {
    Set<EntityType> types = Set.of(EntityType.ARTIST);
    DiscogsDump cached = getRandomDumpWithType(EntityType.ARTIST);
    when(dumpSupplier.getLatest(types)).thenReturn(List.of());
    when(repository.findTopByType(EntityType.ARTIST)).thenReturn(cached);

    assertThat(dumpService.resolveLatest(types)).isEqualTo(List.of(cached));
    verify(repository, never()).saveAll(any());
  }

  @Test
  void existsShouldDelegateToRepository() {
    when(repository.existsByETag("etag")).thenReturn(true);

    assertThat(dumpService.exists("etag")).isTrue();
    verify(repository).existsByETag("etag");
  }

  @Test
  void nullLatestRefreshWithoutDurableCatalogFails() {
    Set<EntityType> types = Set.of(EntityType.ARTIST);
    when(dumpSupplier.getLatest(types)).thenReturn(null);

    assertThat(catchThrowable(() -> dumpService.resolveLatest(types)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("returned no selected dumps");
  }

  @Test
  void successfulLatestRefreshMustBecomeVisibleInTheDurableCatalog() {
    Set<EntityType> types = Set.of(EntityType.ARTIST);
    DiscogsDump selected = getRandomDumpWithType(EntityType.ARTIST);
    when(dumpSupplier.getLatest(types)).thenReturn(List.of(selected));

    assertThat(catchThrowable(() -> dumpService.resolveLatest(types)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessage("failed to locate a dump for every requested entity type");
  }

  @Test
  void exactMonthUsesTheDurableCatalogWithoutAnUpstreamRequest() {
    Set<EntityType> types = Set.of(EntityType.ARTIST);
    YearMonth month = YearMonth.of(2026, 7);
    DiscogsDump cached = getRandomDumpWithType(EntityType.ARTIST, month.atDay(1));
    when(repository.findTopByTypeAndLastModifiedAtBetween(
            EntityType.ARTIST, month.atDay(1), month.plusMonths(1).atDay(1)))
        .thenReturn(cached);

    assertThat(dumpService.resolveMonth(types, month)).isEqualTo(List.of(cached));
    verifyNoInteractions(dumpSupplier);
  }

  @Test
  void freshExactMonthPersistsThePinnedSelectionBeforeReturningIt() {
    Set<EntityType> types = Set.of(EntityType.ARTIST, EntityType.RELEASE);
    YearMonth month = YearMonth.of(2026, 7);
    List<DiscogsDump> selected =
        List.of(
            getRandomDumpWithType(EntityType.ARTIST, month.atDay(1)),
            getRandomDumpWithType(EntityType.RELEASE, month.atDay(1)));
    when(repository.findTopByTypeAndLastModifiedAtBetween(
            EntityType.ARTIST, month.atDay(1), month.plusMonths(1).atDay(1)))
        .thenReturn(null, selected.getFirst());
    when(repository.findTopByTypeAndLastModifiedAtBetween(
            EntityType.RELEASE, month.atDay(1), month.plusMonths(1).atDay(1)))
        .thenReturn(null, selected.getLast());
    when(dumpSupplier.getMonth(types, month)).thenReturn(selected);

    assertThat(dumpService.resolveMonth(types, month)).isEqualTo(selected);
    verify(repository).saveAll(selected);
  }

  @Test
  void exactMonthFailsIfDurablePersistenceDoesNotExposeACompleteSelection() {
    Set<EntityType> types = Set.of(EntityType.LABEL);
    YearMonth month = YearMonth.of(2026, 7);
    DiscogsDump selected = getRandomDumpWithType(EntityType.LABEL, month.atDay(1));
    when(dumpSupplier.getMonth(types, month)).thenReturn(List.of(selected));

    assertThat(catchThrowable(() -> dumpService.resolveMonth(types, month)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("did not persist a complete selection");
  }

  @Test
  void selectedResolutionRejectsAmbiguousInputs() {
    assertThat(catchThrowable(() -> dumpService.resolveLatest(null)))
        .isInstanceOf(InvalidArgumentException.class);
    assertThat(catchThrowable(() -> dumpService.resolveLatest(Set.of())))
        .isInstanceOf(InvalidArgumentException.class);
    assertThat(catchThrowable(() -> dumpService.resolveMonth(Set.of(EntityType.ARTIST), null)))
        .isInstanceOf(InvalidArgumentException.class);
  }

  @Test
  void getDiscogsDumpShouldHandoverSameParameter__ThenReturnsTheSameResult__FromRepository()
      throws DumpNotFoundException {
    String fakeETag = RandomString.make(20);
    DiscogsDump fakeDump = getRandomDump();
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

    when(repository.findByETag(stringCaptor.capture())).thenReturn(fakeDump);

    // when
    DiscogsDump result = dumpService.getDiscogsDump(fakeETag);

    // then
    verify(repository, times(1)).findByETag(fakeETag);
    assertThat(stringCaptor.getValue()).isEqualTo(fakeETag);
    assertThat(result).isEqualTo(fakeDump);
  }

  @Test
  void
  whenGetMostRecentDiscogsDumpByTypeCalled__ThenShouldCallRepositoryOnce__AndShouldHaveProperResult() {
    DiscogsDump fakeDump = getRandomDump();
    EntityType type = fakeDump.getType();
    ArgumentCaptor<EntityType> dumpTypeCaptor = ArgumentCaptor.forClass(EntityType.class);
    when(repository.findTopByType(dumpTypeCaptor.capture()))
        .thenReturn(fakeDump);

    // when
    DiscogsDump result = dumpService.getMostRecentDiscogsDumpByType(type);

    // then
    verify(repository, times(1)).findTopByType(type);
    assertThat(dumpTypeCaptor.getValue()).isEqualTo(type);
    assertThat(result).isEqualTo(fakeDump);
  }

  @Test
  void getDumpByTypeInRange() {
    DiscogsDump fakeDump = getRandomDump();
    EntityType type = fakeDump.getType();

    ArgumentCaptor<EntityType> typeCaptor = ArgumentCaptor.forClass(EntityType.class);
    ArgumentCaptor<LocalDate> localDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    when(repository.findByTypeAndLastModifiedAtBetween(
        typeCaptor.capture(), localDateCaptor.capture(), localDateCaptor.capture()))
        .thenReturn(List.of(fakeDump));

    LocalDate startDate = fakeDump.getLastModifiedAt().withDayOfMonth(1);
    LocalDate endDate = startDate.plusMonths(1);
    int year = startDate.getYear();
    int month = startDate.getMonthValue();

    // when
    List<DiscogsDump> result = dumpService.getDumpByTypeInRange(type, year, month);

    // then
    verify(repository, times(1)).findByTypeAndLastModifiedAtBetween(type, startDate, endDate);
    assertThat(localDateCaptor.getAllValues().get(0)).isEqualTo(startDate);
    assertThat(localDateCaptor.getAllValues().get(1)).isEqualTo(endDate);
    assertThat(result.get(0)).isEqualTo(fakeDump);
  }

  @Test
  void whenGetLatestCompleteDumpSet__ThenReturnsLatestDumpForEachEntity()
      throws io.dsub.discogs.batch.exception.DumpNotFoundException {
    LocalDate baseDate = LocalDate.of(2026, 4, 1);
    List<DiscogsDump> latestDumps =
        IntStream.range(0, 4)
            .mapToObj(
                index ->
                    getRandomDumpWithType(
                        EntityType.values()[index], baseDate.plusMonths(index)))
            .collect(Collectors.toList());
    DiscogsDump olderArtist =
        getRandomDumpWithType(EntityType.ARTIST, baseDate.minusMonths(1));

    List<DiscogsDump> candidates = new ArrayList<>(latestDumps);
    candidates.add(olderArtist);
    when(repository.findAll()).thenReturn(candidates);

    List<DiscogsDump> result = dumpService.getLatestCompleteDumpSet();

    verify(repository, times(1)).findAll();
    assertThat(result).isEqualTo(latestDumps);
  }

  @Test
  void whenAfterPropertiesSet__ThenShouldOnlyValidateDependencies() {
    assertDoesNotThrow(() -> dumpService.afterPropertiesSet());
    verifyNoInteractions(repository, dumpSupplier);
  }

  @Test
  void whenGetLatestCompleteDumpSet__ThenShouldThrowDumpNotFoundException() {
    assertThrows(DumpNotFoundException.class, () -> dumpService.getLatestCompleteDumpSet());
  }

  @Test
  void whenGetAllCalled__ThenShouldCall__RepositoryFindAllMethod() {
    List<DiscogsDump> fakeList = new ArrayList<>();
    when(repository.findAll()).thenReturn(fakeList);

    // when
    assertThat(dumpService.getAll()).isEqualTo(fakeList);
    verify(repository, atMostOnce()).findAll();
  }

  @ParameterizedTest
  @EnumSource(EntityType.class)
  void whenGetMostRecentDiscogsDumpByTypeYearMonth__ThenShouldCallRepositoryWithExpectedValue(
      EntityType type) {
    ArgumentCaptor<LocalDate> startDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<LocalDate> endDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<EntityType> dumpTypeArgumentCaptor = ArgumentCaptor.forClass(EntityType.class);
    DiscogsDump expectedDump = getRandomDump();

    LocalDate startDate = LocalDate.now().minusDays(1000 + random.nextInt(1000)).withDayOfMonth(1);
    LocalDate endDate = startDate.plusMonths(1);

    when(repository.findTopByTypeAndLastModifiedAtBetween(
        dumpTypeArgumentCaptor.capture(), startDateCaptor.capture(), endDateCaptor.capture()))
        .thenReturn(expectedDump);

    // when
    DiscogsDump resultDump =
        dumpService.getMostRecentDiscogsDumpByTypeYearMonth(
            type, startDate.getYear(), startDate.getMonthValue());
    // then
    assertThat(resultDump).isEqualTo(expectedDump);
    assertThat(dumpTypeArgumentCaptor.getValue()).isEqualTo(type);
    assertThat(startDateCaptor.getValue()).isEqualTo(startDate);
    assertThat(endDateCaptor.getValue()).isEqualTo(endDate);
  }

  @Test
  void whenRepositoryNotSet__ThenShouldThrow() {
    dumpService = new DefaultDiscogsDumpService(null, dumpSupplier);

    // when
    Throwable t = catchThrowable(() -> dumpService.afterPropertiesSet());

    // then
    assertThat(t)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessage("repository cannot be null");
  }

  @Test
  void whenDumpSupplierNotSet__ThenShouldThrow() {
    dumpService = new DefaultDiscogsDumpService(repository, null);

    // when
    Throwable t = catchThrowable(() -> dumpService.afterPropertiesSet());

    // then
    assertThat(t)
        .isInstanceOf(InitializationFailureException.class)
        .hasMessage("dumpSupplier cannot be null");
  }

  @Test
  void whenGetAllByTypeYearMonth__WithDuplicatedType__ShouldNotThrow() {
    EntityType type = EntityType.values()[random.nextInt(4)];
    when(repository.findAllByLastModifiedAtIsBetween(any(), ArgumentMatchers.any()))
        .thenReturn(List.of(getRandomDumpWithType(type, LocalDate.of(1, 1, 2))));

    // when
    Assertions.assertDoesNotThrow(
        () -> dumpService.getAllByTypeYearMonth(List.of(type, type), 1, 1));
  }

  @Test
  void whenGetAllByTypeYearMonth__ShouldThrowIfRepositoryReturnsNull() {
    when(repository.findAllByLastModifiedAtIsBetween(any(), ArgumentMatchers.any()))
        .thenReturn(List.of());
    EntityType type = EntityType.values()[random.nextInt(4)];
    Throwable t = catchThrowable(() -> dumpService.getAllByTypeYearMonth(List.of(type), 1, 1));
    assertThat(t)
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessage("dump of type " + type + " from 1-1 not found");
  }

  @Test
  void whenCurrentMonthIsMissing__ShouldReportDistributionDelay() {
    LocalDate now = LocalDate.now();
    when(repository.findAllByLastModifiedAtIsBetween(any(), any())).thenReturn(List.of());

    Throwable throwable = catchThrowable(
        () -> dumpService.getAllByTypeYearMonth(
            List.of(EntityType.ARTIST), now.getYear(), now.getMonthValue()));

    assertThat(throwable)
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessage("dump for current month seems to be missing from distribution.");
  }

  @Test
  void whenAnotherMonthInCurrentYearIsMissing__ShouldReportRequestedType() {
    LocalDate now = LocalDate.now();
    int otherMonth = now.getMonthValue() == 1 ? 2 : 1;
    when(repository.findAllByLastModifiedAtIsBetween(any(), any())).thenReturn(List.of());

    Throwable throwable = catchThrowable(
        () -> dumpService.getAllByTypeYearMonth(
            List.of(EntityType.ARTIST), now.getYear(), otherMonth));

    assertThat(throwable)
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("dump of type artist");
  }

  @Test
  void whenMultipleRequestedTypesAreMissing__ShouldReportSet() {
    when(repository.findAllByLastModifiedAtIsBetween(any(), any())).thenReturn(List.of());

    Throwable throwable = catchThrowable(
        () -> dumpService.getAllByTypeYearMonth(
            List.of(EntityType.ARTIST, EntityType.LABEL), 2008, 3));

    assertThat(throwable)
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("dump set of types");
  }

  @Test
  void whenNoTypesAreRequested__ShouldReportMissingSet() {
    when(repository.findAllByLastModifiedAtIsBetween(any(), any())).thenReturn(List.of());

    assertThat(catchThrowable(() -> dumpService.getAllByTypeYearMonth(List.of(), 2008, 3)))
        .isInstanceOf(DumpNotFoundException.class)
        .hasMessageContaining("dump set of types []");
  }

  @ParameterizedTest
  @EnumSource(EntityType.class)
  void whenGetAllByTypeYearMonth__ShouldReturnProperValue(EntityType type)
      throws io.dsub.discogs.batch.exception.DumpNotFoundException {
    DiscogsDump expectedDump =
        getRandomDumpWithType(type, LocalDate.of(1, 1, 2));
    Collection<DiscogsDump> expected = List.of(expectedDump);
    when(repository.findAllByLastModifiedAtIsBetween(any(), ArgumentMatchers.any()))
        .thenReturn(List.of(expectedDump));

    // when
    Collection<DiscogsDump> result = dumpService.getAllByTypeYearMonth(List.of(type), 1, 1);

    // then
    assertThat(result.size()).isEqualTo(expected.size());
    assertThat(result.iterator().next()).isEqualTo(expected.iterator().next());
    verify(repository, times(1))
        .findAllByLastModifiedAtIsBetween(any(), ArgumentMatchers.any());
  }

  @Test
  void whenNewestDateOmitsDomain__ThenKeepsLatestPerEntity() throws Exception {
    LocalDate coherentDate = LocalDate.of(2024, 1, 1);
    LocalDate incompleteDate = LocalDate.of(2024, 1, 15);
    List<DiscogsDump> coherentSet =
        IntStream.range(0, EntityType.values().length)
            .mapToObj(
                index -> getRandomDumpWithType(EntityType.values()[index], coherentDate))
            .collect(Collectors.toList());
    List<DiscogsDump> incompleteSet =
        IntStream.range(0, EntityType.values().length - 1)
            .mapToObj(
                index -> getRandomDumpWithType(EntityType.values()[index], incompleteDate))
            .collect(Collectors.toList());
    List<DiscogsDump> candidates = new ArrayList<>(coherentSet);
    candidates.addAll(incompleteSet);
    when(repository.findAllByLastModifiedAtIsBetween(any(), any())).thenReturn(candidates);

    Collection<DiscogsDump> result =
        dumpService.getAllByTypeYearMonth(List.of(EntityType.values()), 2024, 1);

    assertThat(result.size()).isEqualTo(EntityType.values().length);
    result.forEach(
        dump ->
            assertThat(dump.getLastModifiedAt())
                .isEqualTo(
                    dump.getType() == EntityType.RELEASE
                        ? coherentDate
                        : incompleteDate));
  }

  @Test
  void whenUnrequestedDomainsAreMissing__ThenReturnsLatestRequestedDomain() throws Exception {
    LocalDate olderDate = LocalDate.of(2024, 1, 1);
    LocalDate latestDate = LocalDate.of(2024, 1, 15);
    DiscogsDump olderArtist = getRandomDumpWithType(EntityType.ARTIST, olderDate);
    DiscogsDump latestArtist = getRandomDumpWithType(EntityType.ARTIST, latestDate);
    DiscogsDump olderLabel = getRandomDumpWithType(EntityType.LABEL, olderDate);
    when(repository.findAllByLastModifiedAtIsBetween(any(), any()))
        .thenReturn(List.of(olderArtist, olderLabel, latestArtist));

    Collection<DiscogsDump> result =
        dumpService.getAllByTypeYearMonth(List.of(EntityType.ARTIST), 2024, 1);

    assertThat(result.size()).isEqualTo(1);
    assertThat(result.iterator().next()).isEqualTo(latestArtist);
  }

  @Test
  void whenAfterPropertiesSetCalled__ShouldThrowIfAnythingIsMissing() {
    DefaultDiscogsDumpService service = new DefaultDiscogsDumpService(repository, dumpSupplier);
    assertDoesNotThrow(service::afterPropertiesSet);

    DefaultDiscogsDumpService secondService = new DefaultDiscogsDumpService(null, dumpSupplier);
    assertThrows(InitializationFailureException.class, secondService::afterPropertiesSet);

    DefaultDiscogsDumpService thirdService = new DefaultDiscogsDumpService(repository, null);
    assertThrows(InitializationFailureException.class, thirdService::afterPropertiesSet);

    DefaultDiscogsDumpService fourthService = new DefaultDiscogsDumpService(null, null);
    assertThrows(InitializationFailureException.class, fourthService::afterPropertiesSet);
  }
}
