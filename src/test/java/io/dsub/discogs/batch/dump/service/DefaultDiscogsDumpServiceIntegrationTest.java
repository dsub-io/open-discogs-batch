package io.dsub.discogs.batch.dump.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.DefaultDumpSupplier;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.repository.DiscogsDumpRepository;
import io.dsub.discogs.batch.dump.repository.MapDiscogsDumpRepository;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.util.ResourceUtils;

public class DefaultDiscogsDumpServiceIntegrationTest {

  List<DiscogsDump> sampleDumpList;

  DumpSupplier dumpSupplier;
  DiscogsDumpRepository repository;
  DiscogsDumpService dumpService;

  @BeforeEach
  void setUp() throws Exception {
    sampleDumpList =
        new DefaultDumpSupplier()
            .get(ResourceUtils.getFile("classpath:test/DiscogsDataDump.xml")).stream()
            .sorted(DiscogsDump::compareTo)
            .collect(Collectors.toList());
    dumpSupplier = Mockito.mock(DumpSupplier.class);
    when(dumpSupplier.get()).thenReturn(sampleDumpList);
    repository = new MapDiscogsDumpRepository(dumpSupplier);
    repository.afterPropertiesSet();
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
      EntityType targetType = EntityType.values()[idx];
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
              .collect(Collectors.groupingBy(DiscogsDump::getType))
              .values()
              .stream()
              .map(dumps -> dumps.stream().max(DiscogsDump::compareTo).orElseThrow())
              .toList();

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
        EntityType type = expectedDump.getType();
        int year = expectedDump.getLastModifiedAt().getYear();
        int month = expectedDump.getLastModifiedAt().getMonthValue();
        // when
        List<DiscogsDump> results = dumpService.getDumpByTypeInRange(type, year, month);
        // then
        assertThat(expectedDump).isIn(results);
      }
    }
  }
}
