package io.dsub.discogs.batch.dump.repository;

import io.dsub.discogs.batch.TestArguments;
import io.dsub.discogs.batch.dump.DefaultDumpSupplier;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class DiscogsDumpRepositoryUnitTest {

    DumpSupplier dumpSupplier;

    static List<DiscogsDump> DUMP_LIST;

    static String T_FILE_PATH = "classpath:test/DiscogsDataDump.xml";

    private MapDiscogsDumpRepository repository;


    @BeforeAll
    static void beforeAll() throws FileNotFoundException {
        DUMP_LIST = new DefaultDumpSupplier().get(ResourceUtils.getFile(T_FILE_PATH));
    }

    @BeforeEach
    void setUp() throws Exception {
        dumpSupplier = Mockito.mock(DumpSupplier.class);
        doReturn(DUMP_LIST).when(dumpSupplier).get();
        doReturn(DUMP_LIST).when(dumpSupplier).get(any());
        repository = spy(new MapDiscogsDumpRepository(dumpSupplier));
        repository.afterPropertiesSet();
    }

    @ParameterizedTest
    @MethodSource("io.dsub.discogs.batch.TestArguments#getLocalDateTimes")
    void whenFindAllByLastModifiedAtIsBetween__ResultsMustStayTheSame(LocalDate ldt) {
        // when
        List<DiscogsDump> result = repository
                .findAllByLastModifiedAtIsBetween(ldt, ldt.plusMonths(1));

        // then
        result.forEach(dump -> assertThat(dump.getLastModifiedAt())
                .isBetween(ldt, ldt.plusMonths(1)));
    }

    @Test
    void whenFindAll__ShouldNotReturnEmptyList() {
        // when
        List<DiscogsDump> dumpList = repository.findAll();

        // then
        assertThat(dumpList).isNotEmpty();
    }

    @Test
    void whenCountItemsAfter__ShouldReturnValidCount() {
        LocalDate ld = TestArguments.getLocalDateFrom(2015, 1, 1);
        // when
        int count = repository.countItemsAfter(ld);
        int expected = (int) DUMP_LIST.stream()
                .filter(dump -> dump.getLastModifiedAt().isEqual(ld) || dump.getLastModifiedAt().isAfter(ld))
                .count();
        // then
        assertThat(count).isEqualTo(expected);
    }

    @Test
    void countItemsBefore__ShouldReturnValidCount() {
        LocalDate ld = TestArguments.getLocalDateFrom(2015, 1, 1);
        // when
        int count = repository.countItemsBefore(ld);
        int expected = (int) DUMP_LIST.stream()
                .filter(dump -> dump.getLastModifiedAt().isEqual(ld) || dump.getLastModifiedAt().isBefore(ld))
                .count();
        // then
        assertThat(count).isEqualTo(expected);
    }

    @Test
    void countItemsBetween__ShouldReturnValidCount() {
        LocalDate start = TestArguments.getLocalDateFrom(2015, 1, 1);
        LocalDate end = start.plusMonths(1);
        // when
        int count = repository.countItemsBetween(start, end);
        int expected = (int) DUMP_LIST.stream()
                .filter(dump -> dump.getLastModifiedAt().isBefore(end))
                .filter(dump -> dump.getLastModifiedAt().isEqual(start) || dump.getLastModifiedAt().isAfter(start))
                .count();
        // then
        assertThat(count).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("io.dsub.discogs.batch.TestArguments#getLocalDateTimes")
    void whenFindTopByTypeAndLastModifiedAtBetween__ShouldOnlyPresentItemsWithGivenTimeframe(LocalDate ldt) {
        for (EntityType type : EntityType.values()) {
            DiscogsDump dump = repository.findTopByTypeAndLastModifiedAtBetween(type, ldt, ldt.plusMonths(1));
            if (dump != null) {
                assertThat(dump.getLastModifiedAt()).isBetween(ldt, ldt.plusMonths(1));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("io.dsub.discogs.batch.TestArguments#getLocalDateTimes")
    void whenFindByTypeAndLastModifiedAtBetween__ShouldReturnValidItems(LocalDate ldt) {
        for (EntityType type : EntityType.values()) {
            List<DiscogsDump> result = repository.findByTypeAndLastModifiedAtBetween(type, ldt, ldt.plusMonths(1));

            result.forEach(dump -> assertThat(dump.getLastModifiedAt()).isBetween(ldt, ldt.plusMonths(1)));
        }
    }

    @Test
    void whenFindTopByTypeAndLastModifiedAtBetween__ShouldReturnMostRecentItem() {
        LocalDate ld = LocalDate.of(1995, 5, 1);

        DiscogsDump prev = TestArguments.getRandomDumpWithType(EntityType.ARTIST, ld);
        DiscogsDump latter = TestArguments.getRandomDumpWithType(EntityType.ARTIST, ld.plusDays(1));

        repository.save(prev);
        repository.save(latter);

        // when
        DiscogsDump dump = repository.findTopByTypeAndLastModifiedAtBetween(EntityType.ARTIST, ld, ld.plusMonths(1));

        // then
        assertThat(dump).isEqualTo(latter);
    }

    @Test
    void whenFindTopByTypeAndLastModifiedAt__ShouldReturnMostRecentOne() {
        DiscogsDump expected = DUMP_LIST.stream()
                .filter(dump -> dump.getType().equals(EntityType.ARTIST))
                .max(DiscogsDump::compareTo)
                .orElse(null);

        // when
        DiscogsDump found = repository.findTopByType(EntityType.ARTIST);

        // then
        assertThat(found).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("eTagStream")
    void whenFindByETag__ShouldGetProperResult(String eTag) {
        // when
        DiscogsDump found = repository.findByETag(eTag);

        // then
        assertThat(found.getETag()).isEqualTo(eTag);
    }

    @Test
    void givenETagIsNullOrEmpty__WhenFindByETag__ShouldReturnNull() {
        // when
        DiscogsDump found = repository.findByETag("");

        // then
        assertThat(found).isNull();

        // when
        found = repository.findByETag(null);

        // then
        assertThat(found).isNull();
    }

    @Test
    void givenETagIsInvalid__WhenFindByETag__ShouldReturnNull() {
        // when
        DiscogsDump found = repository.findByETag("Oh man..");

        // then
        assertThat(found).isNull();
    }

    @Test
    void whenCount__ShouldReturnValidCount() {
        int expected = DUMP_LIST.size();

        // when
        int actual = repository.count();

        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void whenSaveAllWithNullOrBlankList__ShouldNotThrow() {
        assertDoesNotThrow(() -> repository.saveAll(null));
        assertDoesNotThrow(() -> repository.saveAll(new ArrayList<>()));
    }

    @Test
    void whenSaveAll__ShouldSaveProperly() {
        List<DiscogsDump> toAdd = IntStream.range(0, 10)
                .mapToObj(i -> TestArguments.getRandomDump())
                .collect(Collectors.toList());

        // when
        repository.saveAll(toAdd);

        // then
        assertThat(repository.findAll()).containsAll(toAdd);
    }

    @Test
    void whenDeleteAll__ShouldClearAllItems() {
        assertThat(repository.count()).isGreaterThan(0);

        // when
        repository.deleteAll();

        // then
        assertThat(repository.count()).isZero();
    }

    @ParameterizedTest
    @MethodSource("eTagStream")
    void whenExistsByEtag__ShouldBeTrue(String eTag) {
        // when
        boolean exists = repository.existsByETag(eTag);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void givenETagDoesNotExists__WhenExistsByEtag__ShouldBeFalse() {
        // when
        boolean exists = repository.existsByETag("sade adu");

        // then
        assertThat(exists).isFalse();
    }

    private static Stream<Arguments> eTagStream() {
        return DUMP_LIST.stream()
                .map(DiscogsDump::getETag)
                .map(Arguments::of);
    }
}