package io.dsub.discogsdata.batch.dump.service;

import ch.qos.logback.classic.spi.ILoggingEvent;
import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpSupplier;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.batch.dump.repository.DiscogsDumpRepository;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import io.dsub.discogsdata.common.exception.DumpNotFoundException;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DefaultDiscogsDumpServiceUnitTest {

  final Random random = new Random();
  @RegisterExtension public LogSpy logSpy = new LogSpy();
  @Mock DiscogsDumpRepository repository;
  @Mock DumpSupplier dumpSupplier;
  @InjectMocks DefaultDiscogsDumpService dumpService;
  @Captor private ArgumentCaptor<List<DiscogsDump>> dumpListCaptor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void whenUpdateDB__ShouldFilterNullValues__BeforeCallDumpRepository__SaveAllMethod() {
    List<DiscogsDump> listIncludingNull =
        IntStream.range(0, 10 + random.nextInt(10))
            .mapToObj(n -> n % 3 == 0 ? null : getRandomDump())
            .collect(Collectors.toList());

    List<DiscogsDump> filtered =
        listIncludingNull.stream().filter(Objects::nonNull).collect(Collectors.toList());

    assertThat(listIncludingNull.contains(null)).isTrue();
    when(dumpSupplier.get()).thenReturn(listIncludingNull);
    when(repository.saveAll(dumpListCaptor.capture())).thenReturn(null);

    // when
    dumpService.updateDB();

    // then
    verify(dumpSupplier, times(1)).get();
    verify(repository, times(1)).saveAll(filtered);
    assertThat(dumpListCaptor.getValue())
        .isNotNull()
        .isNotSameAs(listIncludingNull)
        .isEqualTo(filtered);
  }

  @Test
  void whenUpdateDBWithNull__ShouldNotCallRepositoryMethod() {
    when(dumpSupplier.get()).thenReturn(null);
    dumpService.updateDB();
    verify(dumpSupplier, times(1)).get();
    verify(repository, times(0)).saveAll(any());
    List<ILoggingEvent> logs = logSpy.getEvents();
    assertThat(logs.size()).isEqualTo(1);
    assertThat(logs.get(0).getMessage())
        .isEqualTo("failed to fetch items via DumpSupplier. cancelling the update...");
  }

  @Test
  void whenUpdateDB__ShouldCallProperDelegatedMethodsWithValues() {
    List<DiscogsDump> dumpList =
        IntStream.range(0, 10 + random.nextInt(10))
            .mapToObj(n -> getRandomDump())
            .collect(Collectors.toList());

    // when
    when(dumpSupplier.get()).thenReturn(dumpList);
    when(repository.saveAll(dumpListCaptor.capture())).thenReturn(dumpList);
    dumpService.updateDB();

    // then
    verify(dumpSupplier, times(1)).get();
    verify(repository, times(1)).saveAll(dumpList);
    assertThat(dumpListCaptor.getValue()).isEqualTo(dumpList);
  }

  @Test
  void getDiscogsDumpShouldHandoverSameParameter__ThenReturnsTheSameResult__FromRepository() {
    String fakeETag = RandomString.make(20);
    DiscogsDump fakeDump = getRandomDump();
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

    when(repository.findByeTag(stringCaptor.capture())).thenReturn(fakeDump);

    // when
    DiscogsDump result = dumpService.getDiscogsDump(fakeETag);

    // then
    verify(repository, times(1)).findByeTag(fakeETag);
    assertThat(stringCaptor.getValue()).isEqualTo(fakeETag);
    assertThat(result).isEqualTo(fakeDump);
  }

  @Test
  void
      whenGetMostRecentDiscogsDumpByTypeCalled__ThenShouldCallRepositoryOnce__AndShouldHaveProperResult() {
    DiscogsDump fakeDump = getRandomDump();
    DumpType type = fakeDump.getType();
    ArgumentCaptor<DumpType> dumpTypeCaptor = ArgumentCaptor.forClass(DumpType.class);
    when(repository.findTopByTypeOrderByCreatedAtDesc(dumpTypeCaptor.capture()))
        .thenReturn(fakeDump);

    // when
    DiscogsDump result = dumpService.getMostRecentDiscogsDumpByType(type);

    // then
    verify(repository, times(1)).findTopByTypeOrderByCreatedAtDesc(type);
    assertThat(dumpTypeCaptor.getValue()).isEqualTo(type);
    assertThat(result).isEqualTo(fakeDump);
  }

  @Test
  void getDumpByTypeInRange() {
    DiscogsDump fakeDump = getRandomDump();
    DumpType type = fakeDump.getType();

    ArgumentCaptor<DumpType> typeCaptor = ArgumentCaptor.forClass(DumpType.class);
    ArgumentCaptor<LocalDate> localDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
    when(repository.findByTypeAndCreatedAtBetween(
            typeCaptor.capture(), localDateCaptor.capture(), localDateCaptor.capture()))
        .thenReturn(List.of(fakeDump));

    LocalDate startDate = fakeDump.getCreatedAt().withDayOfMonth(1);
    LocalDate endDate = startDate.plusMonths(1).minusDays(1);
    int year = startDate.getYear();
    int month = startDate.getMonthValue();

    // when
    List<DiscogsDump> result = dumpService.getDumpByTypeInRange(type, year, month);

    // then
    verify(repository, times(1)).findByTypeAndCreatedAtBetween(type, startDate, endDate);
    assertThat(localDateCaptor.getAllValues().get(0)).isEqualTo(startDate);
    assertThat(localDateCaptor.getAllValues().get(1)).isEqualTo(endDate);
    assertThat(result.get(0)).isEqualTo(fakeDump);
  }

  @Test
  void whenGetLatestCompleteDumpSet__ThenShouldRetryWithPreviousMonths__IfCountIsLowerThan4() {
    LocalDate existingDate = LocalDate.now(Clock.systemUTC()).minusMonths(2);

    List<DiscogsDump> fakeDumps =
        IntStream.range(0, 4)
            .mapToObj(n -> getRandomDumpWithType(DumpType.values()[n]))
            .peek(dump -> dump.setCreatedAt(existingDate))
            .collect(Collectors.toList());

    LocalDate firstStartDate = LocalDate.now(Clock.systemUTC()).withDayOfMonth(1);
    LocalDate firstEndDate = firstStartDate.plusMonths(1);

    LocalDate secondStartDate = firstStartDate.minusMonths(1);
    LocalDate secondEndDate = firstEndDate.minusMonths(1);

    when(repository.countAllByCreatedAtIsBetween(firstStartDate, firstEndDate)).thenReturn(2);
    when(repository.countAllByCreatedAtIsBetween(secondStartDate, secondEndDate)).thenReturn(4);
    when(repository.findAllByCreatedAtIsBetween(secondStartDate, secondEndDate))
        .thenReturn(fakeDumps);

    List<DiscogsDump> result = dumpService.getLatestCompleteDumpSet();

    verify(repository, times(1)).countAllByCreatedAtIsBetween(firstStartDate, firstEndDate);
    verify(repository, times(0)).findAllByCreatedAtIsBetween(firstStartDate, firstEndDate);
    verify(repository, times(1)).countAllByCreatedAtIsBetween(secondStartDate, secondEndDate);
    verify(repository, times(1)).findAllByCreatedAtIsBetween(secondStartDate, secondEndDate);

    assertThat(result).isEqualTo(fakeDumps);
  }

  @Test
  void whenGetLatestCompleteDumpSet__ThenShouldThrowDumpNotFoundException() {
    for (int i = 0; i < 4; i++) {
      when(repository.countAllByCreatedAtIsBetween(any(), any())).thenReturn(i);
      assertThrows(DumpNotFoundException.class, () -> dumpService.getLatestCompleteDumpSet());
    }
  }

  @Test
  void whenGetAllCalled__ThenShouldCall__RepositoryFindAllMethod() {
    List<DiscogsDump> fakeList = new ArrayList<>();
    when(repository.findAll()).thenReturn(fakeList);

    // when
    assertThat(dumpService.getAll()).isEqualTo(fakeList);
    verify(repository, atMostOnce()).findAll();
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

  DiscogsDump getRandomDump() {
    return getRandomDumpWithType(DumpType.values()[random.nextInt(4)]);
  }

  DiscogsDump getRandomDumpWithType(DumpType type) {
    return DiscogsDump.builder()
        .type(type)
        .eTag(RandomString.make(19))
        .createdAt(
            LocalDate.now(Clock.systemUTC())
                .minusYears(random.nextInt(10))
                .minusDays(random.nextInt(10))
                .minusMonths(random.nextInt(19)))
        .size(random.nextLong())
        .uriString(RandomString.make(30))
        .build();
  }
}
