package io.dsub.discogsdata.batch.util;

import io.dsub.discogsdata.batch.dump.DefaultDumpDependencyResolver;
import io.dsub.discogsdata.batch.dump.DumpItem;
import io.dsub.discogsdata.batch.dump.DumpServiceImpl;
import io.dsub.discogsdata.batch.dump.DumpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SimpleDiscogsBatchJobParameterResolverTest {

    @InjectMocks
    private SimpleDiscogsBatchJobParameterResolver parameterResolver;
    @Mock
    private DefaultDumpDependencyResolver dependencyResolver;
    @Mock
    private DumpServiceImpl dumpService;

    private DumpItem artistDump;
    private DumpItem releaseDump;
    private DumpItem masterDump;
    private DumpItem labelDump;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        artistDump = new DumpItem();
        releaseDump = new DumpItem();
        masterDump = new DumpItem();
        labelDump = new DumpItem();

        artistDump.setDumpType(DumpType.ARTIST);
        releaseDump.setDumpType(DumpType.RELEASE);
        masterDump.setDumpType(DumpType.MASTER);
        labelDump.setDumpType(DumpType.LABEL);

        for (DumpItem dump : new DumpItem[]{artistDump, releaseDump, labelDump, masterDump}) {
            dump.setETag(dump.getDumpType().name() + "-etag");
        }
    }


    @Test
    void resolveByEtag() {
        List<String> etags = List.of("a", "b", "c");
        when(dependencyResolver.resolveByETags(etags))
                .thenReturn(List.of(artistDump, labelDump, masterDump));

        JobParameters etagParams = new JobParametersBuilder()
                .addString("etag", "a,b,c")
                .toJobParameters();

        assertThat(parameterResolver.resolveDependencies(etagParams).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveEmptyParameter() {
        List<DumpItem> dumpList = new ArrayList<>();
        dumpList.add(artistDump);
        dumpList.add(releaseDump);
        dumpList.add(labelDump);
        dumpList.add(masterDump);

        when(dumpService.getLatestCompletedDumpSet())
                .thenReturn(dumpList);

        JobParameters emptyJobParameters = new JobParameters();
        emptyJobParameters = parameterResolver.resolveDependencies(emptyJobParameters);
        assertThat(emptyJobParameters.getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("release")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveArtistParameter() {
        JobParameters artistTypeParam = new JobParametersBuilder()
                .addString("types", "artist")
                .toJobParameters();

        List<DumpItem> artistDumpList = new ArrayList<>();
        artistDumpList.add(artistDump);

        when(dependencyResolver.resolveByTypes(any()))
                .thenReturn(artistDumpList);

        assertThat(parameterResolver.resolveDependencies(artistTypeParam).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveReleaseParameter() {
        JobParameters releaseTypeParam = new JobParametersBuilder()
                .addString("types", "release")
                .toJobParameters();
        List<DumpItem> releaseDumpList = Arrays.asList(artistDump, masterDump, releaseDump, labelDump);
        when(dependencyResolver.resolveByTypes(any())).thenReturn(releaseDumpList);
        assertThat(parameterResolver.resolveDependencies(releaseTypeParam).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("release")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveYearMonthParameter() {
        JobParameters yearMonthParam = new JobParametersBuilder()
                .addString("YEARMONTH", "1992-04")
                .toJobParameters();
        when(dependencyResolver.resolveByYearMonth(anyString()))
                .thenReturn(Arrays.asList(artistDump, masterDump, labelDump, releaseDump));

        assertThat(parameterResolver.resolveDependencies(yearMonthParam).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("release")
                .hasFieldOrProperty("master")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveYearMonthAndTypeParameter() {
        JobParameters parameters = new JobParametersBuilder()
                .addString("YEARmONTH", "1992-04")
                .addString("types", "artist,label")
                .toJobParameters();

        List<String> typeParams = Arrays.asList("artist", "label");

        when(dependencyResolver.resolveByTypesAndYearMonth(typeParams, "1992-04"))
                .thenReturn(Arrays.asList(artistDump, labelDump));

        assertThat(parameterResolver.resolveDependencies(parameters).getParameters())
                .hasFieldOrProperty("artist")
                .hasFieldOrProperty("label")
                .hasFieldOrProperty("chunkSize");
    }

    @Test
    void resolveChunkSizeParameter() {
        JobParameters chunkSizeParam = new JobParametersBuilder()
                .addLong("chunkSize", 1000L)
                .toJobParameters();
        assertThat(parameterResolver.resolveDependencies(chunkSizeParam).getLong("chunksize"))
                .isNotNull()
                .isEqualTo(1000L);
    }
}