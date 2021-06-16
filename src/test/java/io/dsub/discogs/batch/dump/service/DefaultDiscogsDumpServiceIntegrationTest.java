package io.dsub.discogs.batch.dump.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.condition.RequiresDiscogsDataConnection;
import io.dsub.discogs.batch.dump.DefaultDumpSupplier;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.DumpType;
import io.dsub.discogs.batch.dump.repository.DiscogsDumpRepository;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@ExtendWith(RequiresDiscogsDataConnection.class)
public class DefaultDiscogsDumpServiceIntegrationTest {

  List<DiscogsDump> sampleDumpList =
      new DefaultDumpSupplier()
          .get().stream()
              .sorted(DiscogsDump::compareTo)
              .skip(200)
              .limit(10)
              .collect(Collectors.toList());

  @Autowired TestEntityManager em;
  @Autowired DiscogsDumpRepository repository;
  @Mock DumpSupplier dumpSupplier;
  DiscogsDumpService dumpService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(dumpSupplier.get()).thenReturn(sampleDumpList);
    repository.deleteAll();
    dumpService = new DefaultDiscogsDumpService(repository, dumpSupplier);
  }

  @Test
  void whenUpdate__ThenShouldNotHaveMissedAnyDumpRecord() {
    // when
    dumpService.updateDB();
    long itemCount =
        repository.findAll().stream().filter(item -> !sampleDumpList.contains(item)).count();

    // then
    assertThat(itemCount).isEqualTo(0);
  }

  @Test
  void whenCallGetAllAfterUpdate__ThenShouldReturnEntireDumpProperly() {
    dumpService.updateDB();
    List<DiscogsDump> result = dumpService.getAll();
    long missingCount = sampleDumpList.stream().filter(item -> !result.contains(item)).count();

    assertThat(result.size()).isEqualTo(sampleDumpList.size());
    assertThat(missingCount).isEqualTo(0);
  }

  @Nested
  class TestsRequiringInitialData {

    @BeforeEach
    void setUp() {
      repository.deleteAll();
      repository.saveAll(sampleDumpList);
    }

    @Test
    void whenCallGetDiscogsDumpWithETag__ThenShouldReturnCorrespondedOne() {
      sampleDumpList.stream()
          .map(DiscogsDump::getETag)
          .peek(
              eTag -> {
                try {
                  assertThat(dumpService.getDiscogsDump(eTag))
                      .isNotNull()
                      .satisfies(dump -> assertThat(dump.getETag()).isEqualTo(eTag));
                } catch (DumpNotFoundException e) {
                  fail(e);
                }
              })
          .close();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void whenCallMostRecentDiscogsDumpByType__ThenShouldReturnMostRecentResult(int idx) {
      DumpType targetType = DumpType.values()[idx];
      DiscogsDump want =
          sampleDumpList.stream()
              .filter(dump -> dump.getType().equals(targetType))
              .max(DiscogsDump::compareTo)
              .orElse(null);
      // when
      DiscogsDump result = dumpService.getMostRecentDiscogsDumpByType(targetType);

      // then
      assertThat(result).isEqualTo(want);
    }

    @Test
    void whenGetLatestCompleteDumpSet__ThenShouldReturnAllRecentDumpsWithEachTypes()
        throws DumpNotFoundException {
      List<DiscogsDump> recentDumps =
          sampleDumpList.stream()
              .collect(
                  Collectors.groupingBy(
                      DiscogsDump::getType, Collectors.reducing(this::reduceAsRecent)))
              .values()
              .stream()
              .filter(Optional::isPresent)
              .map(Optional::get)
              .collect(Collectors.toList());

      // when
      List<DiscogsDump> result = dumpService.getLatestCompleteDumpSet();

      // then
      assertThat(result)
          .satisfies(resultItems -> assertThat(resultItems.size()).isEqualTo(recentDumps.size()))
          .satisfies(
              resultItems -> resultItems.forEach(item -> assertThat(item).isIn(recentDumps)));
    }

    @Test
    void whenDumpByTypeInRangeCalled__ThenShouldContainCorrectItems() {
      for (DiscogsDump expectedDump : sampleDumpList) {
        DumpType type = expectedDump.getType();
        int year = expectedDump.getCreatedAt().getYear();
        int month = expectedDump.getCreatedAt().getMonthValue();
        // when
        List<DiscogsDump> results = dumpService.getDumpByTypeInRange(type, year, month);
        // then
        assertThat(expectedDump).isIn(results);
      }
    }

    private DiscogsDump reduceAsRecent(DiscogsDump a, DiscogsDump b) {
      int n = a.compareTo(b);
      if (n > 0) {
        return a;
      } else {
        return b;
      }
    }
  }
}
