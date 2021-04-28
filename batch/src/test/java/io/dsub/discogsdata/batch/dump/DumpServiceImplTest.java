package io.dsub.discogsdata.batch.dump;

import io.dsub.discogsdata.batch.exception.DumpNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DumpServiceImplTest {

    @Mock
    DumpRepository dumpRepository;

    @Mock
    DumpFetcher dumpFetcher;

    @InjectMocks
    DumpServiceImpl dumpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDumpByEtag() {
        String etagString = "xxxx";
        DumpItem dump = DumpItem.builder().eTag(etagString).build();
        when(dumpRepository.existsByeTag(etagString))
                .thenReturn(true);
        when(dumpRepository.findByeTag(etagString))
                .thenReturn(dump);

        assertThat(dumpService.getDumpByEtag(etagString))
                .isEqualTo(dump);

        assertThrows(DumpNotFoundException.class, () -> dumpService.getDumpByEtag("random"), "dump with etag random not found");
    }

    @Test
    void getMostRecentDumpByType() {
        given(dumpRepository.findTopByDumpTypeOrderByIdDesc(DumpType.ARTIST))
                .willReturn(null);
    }

    @Test
    void getLatestCompletedDumpSet() {
        given(dumpRepository.saveAll(any()))
                .willReturn(null);

        DumpItem dump = new DumpItem();

        given(dumpRepository.findAllByLastModifiedIsBetween(any(), any()))
                .willReturn(List.of(dump, dump, dump, dump));

        List<DumpItem> list = dumpService.getLatestCompletedDumpSet();
        list.forEach(item -> assertEquals(item, dump));

        verify(dumpRepository)
                .findAllByLastModifiedIsBetween(any(), any());
    }

    @Test
    void getDumpListInRange() {
        LocalDateTime start = LocalDateTime.MIN;
        LocalDateTime end = LocalDateTime.MAX;
        List<DumpItem> list = List.of(new DumpItem());
        given(dumpRepository.findAllByLastModifiedIsBetween(start, end))
                .willReturn(list);

        assertEquals(list, dumpService.getDumpListInRange(start, end));
        verify(dumpRepository).findAllByLastModifiedIsBetween(start, end);
    }

    @Test
    void getDumpByDumpTypeInRange() {
        LocalDateTime start = LocalDateTime.MIN;
        LocalDateTime end = LocalDateTime.MAX;
        DumpItem artistDump = new DumpItem();
        given(dumpRepository.findByDumpTypeAndLastModifiedIsBetween(DumpType.ARTIST, start, end))
                .willReturn(artistDump);
        assertEquals(artistDump, dumpService.getDumpByDumpTypeInRange(DumpType.ARTIST, start, end));
        verify(dumpRepository).findByDumpTypeAndLastModifiedIsBetween(DumpType.ARTIST, start, end);
    }

    @Test
    void getDumpListInYearMonth() {
        List<DumpItem> dumpList = List.of(new DumpItem());
        given(dumpRepository.findAllByLastModifiedIsBetween(any(), any()))
                .willReturn(dumpList);
        assertEquals(dumpList, dumpService.getDumpListInYearMonth(1992, 5));
        verify(dumpRepository).findAllByLastModifiedIsBetween(any(), any());
    }

    @Test
    void updateDumps() {
        DumpItem dump = new DumpItem();
        List<DumpItem> dummy = List.of(dump, dump, dump, dump);
        given(dumpFetcher.getDiscogsDumps())
                .willReturn(dummy);
        given(dumpRepository.count())
                .willReturn(4L);
        dumpService.updateDumps();
        verify(dumpFetcher).getDiscogsDumps();
        verify(dumpRepository).count();

        dumpService = new DumpServiceImpl(dumpRepository, dumpFetcher);

        dump.setLastModified(LocalDateTime.now());
        dummy = List.of(dump, dump, dump, dump, dump);
        given(dumpFetcher.getDiscogsDumps())
                .willReturn(dummy);
        given(dumpRepository.count())
                .willReturn(0L);
        dumpService.updateDumps();
        verify(dumpRepository).saveAll(any());
    }

    @Test
    void getAllDumps() {
        DumpItem dump = new DumpItem();
        given(dumpRepository.saveAll(any()))
                .willReturn(null);
        given(dumpRepository.count()).willReturn(4L);
        given(dumpFetcher.getDiscogsDumps())
                .willReturn(List.of(dump, dump, dump, dump));
        dumpService.getAllDumps();

        verify(dumpRepository).findAll();
    }

    @Test
    void isExistsByEtag() {
        given(dumpRepository.existsByeTag(any()))
                .willReturn(false);

        assertFalse(dumpService.isExistsByEtag("testString"));
        verify(dumpRepository)
                .existsByeTag("testString");

        given(dumpRepository.existsByeTag("testString"))
                .willReturn(true);
        assertTrue(dumpService.isExistsByEtag("testString"));
    }
}