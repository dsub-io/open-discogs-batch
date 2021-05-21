package io.dsub.discogsdata.batch.dump.repository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class DiscogsDumpRepositoryMixedTest {

  final Random random = new Random();
  @Autowired
  TestEntityManager em;
  @Autowired
  DiscogsDumpRepository repository;
  private List<DiscogsDump> persistedDiscogsDumpList;

  private DiscogsDump getRandomDump() {
    return getRandomDumpWithCreatedAt(
        LocalDate.now()
            .minusYears(1 + random.nextInt(30))
            .minusMonths(random.nextInt(13))
            .minusDays(random.nextInt(40)));
  }

  private DiscogsDump getRandomDumpWithCreatedAt(LocalDate createdAt) {
    return DiscogsDump.builder()
        .type(DumpType.ARTIST)
        .eTag(RandomString.make(10))
        .size(random.nextLong())
        .uriString(RandomString.make(10))
        .createdAt(createdAt)
        .build();
  }

  @Nested
  class StandaloneTests {

    @Test
    void whenDuplicatedButMoreRecentDumpExists__ShouldReturnMoreRecent() {
      // preps
      repository.deleteAll();
      LocalDate recent = LocalDate.of(2010, 3, 10);
      LocalDate older = recent.minusDays(3);
      List<DiscogsDump> items =
          IntStream.range(0, 5)
              .mapToObj(i -> i < 4 ? recent : older)
              .map(DiscogsDumpRepositoryMixedTest.this::getRandomDumpWithCreatedAt)
              .collect(Collectors.toList());
      repository.saveAll(items);
      assertThat(repository.count()).isEqualTo(5); // ensure we have 5

      // when
      List<DiscogsDump> dumps =
          repository.findAllByCreatedAtIsBetween(recent, recent.plusMonths(1));

      // then
      assertThat(dumps.size()).isEqualTo(4);
      dumps.forEach(dump -> assertThat(dump.getCreatedAt()).isNotEqualTo(older));
    }
  }

  @Nested
  class InitializedTests {

    @BeforeEach
    void setUp() {
      repository.deleteAll();
      persistedDiscogsDumpList =
          repository.saveAll(
              IntStream.range(0, random.nextInt(30))
                  .mapToObj(i -> getRandomDump())
                  .collect(Collectors.toList()));
    }

    @Test
    void whenExistsByeTag__ThenReturnsProperBoolean() {
      persistedDiscogsDumpList.forEach(
          persistedDump -> assertThat(repository.existsByeTag(persistedDump.getETag())).isTrue());

      assertThat(repository.existsByeTag("i do not exists...")).isFalse();
    }

    @Test
    void whenFindByeTag__ThenShouldReturnSameValue() {
      for (DiscogsDump dump : persistedDiscogsDumpList) {
        String targetETag = dump.getETag();
        assertThat(repository.findByeTag(targetETag))
            .isNotNull()
            .extracting(DiscogsDump::getETag)
            .isEqualTo(targetETag);
      }
    }

    @Test
    void whenFindTopByTypeOrderByCreatedAtDesc__ShouldReturnMostRecentDumpByCreatedAt() {
      persistedDiscogsDumpList.sort(DiscogsDump::compareTo);
      persistedDiscogsDumpList.stream()
          .map(DiscogsDump::getCreatedAt)
          .skip(persistedDiscogsDumpList.size() - 1) // the most recent will be in the last order.
          .findFirst()
          .ifPresent(
              mostRecentDate -> {
                DiscogsDump thisDump = // more recent one.
                    getRandomDumpWithCreatedAt(mostRecentDate.plusYears(1).minusMonths(1));
                repository.saveAndFlush(thisDump);
                DiscogsDump thatDump =
                    repository.findTopByTypeOrderByCreatedAtDesc(thisDump.getType());
                assertThat(thisDump).isEqualTo(thatDump);
              });
    }

    @Test
    void whenFindAllByCreatedAtIsBetween__WillReturnTheProperValues() {
      DiscogsDump mid =
          persistedDiscogsDumpList.stream()
              .skip(persistedDiscogsDumpList.size() / 2)
              .findFirst()
              .orElse(null);
      assertThat(mid).isNotNull();

      Optional<DiscogsDump> expectedPresent =
          repository
              .findAllByCreatedAtIsBetween(mid.getCreatedAt(), mid.getCreatedAt().plusMonths(1))
              .stream()
              .filter(dump -> dump.equals(mid))
              .findFirst();

      assertThat(expectedPresent.isPresent()).isTrue();
      assertThat(expectedPresent.get()).isEqualTo(mid);
    }

    @Test
    void whenFindAllByTypeAndCreatedAtIsBetween__ShouldReturnTheCorrectList() {

      DiscogsDump mid = // fetch middle item
          persistedDiscogsDumpList.stream()
              .skip(persistedDiscogsDumpList.size() / 2)
              .findFirst()
              .orElse(null);
      assertThat(mid).isNotNull();

      DumpType type = mid.getType();
      LocalDate createdAt = mid.getCreatedAt();

      // when
      List<DiscogsDump> found =
          repository.findByTypeAndCreatedAtBetween(type, createdAt, createdAt.plusMonths(1));

      // then
      assertThat(found.get(0))
          .isNotNull() // should exists
          .isEqualTo(mid); // should equals to the mid item we found...
    }

    @Test
    void countAllByCreatedAtIsBetween() {
      List<LocalDate> startDateList =
          persistedDiscogsDumpList.stream()
              .map(DiscogsDump::getCreatedAt)
              .map(LocalDate::getYear)
              .distinct()
              .map(year -> LocalDate.of(year, 1, 1))
              .collect(Collectors.toList());

      for (LocalDate startDate : startDateList) {
        repository
            .findAllByCreatedAtIsBetween(startDate, startDate.plusYears(1))
            .forEach(
                found ->
                    assertThat(found.getCreatedAt())
                        .isAfterOrEqualTo(startDate)
                        .isBefore(startDate.plusYears(1)));
      }
    }
  }
}
