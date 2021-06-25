package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.common.artist.repository.ArtistRepository;
import io.dsub.discogs.common.entity.view.LongIdView;
import io.dsub.discogs.common.label.repository.LabelRepository;
import io.dsub.discogs.common.master.repository.MasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class IdCachingJobExecutionListener implements JobExecutionListener {

    protected static final String ARTIST = "artist";
    protected static final String LABEL = "label";
    protected static final String MASTER = "master";
    protected static final String RELEASE = "release";
    protected static final String STRICT = ArgType.STRICT.getGlobalName();

    private final EntityIdRegistry idRegistry;
    private final ArtistRepository artistRepository;
    private final LabelRepository labelRepository;
    private final MasterRepository masterRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        Map<String, ?> params = jobExecution.getJobParameters().getParameters();

        // caching only required in strict mode (i.e. skipping required steps)
        if (params.containsKey(STRICT)) {
            return;
        }

        boolean doArtist = params.containsKey(ARTIST);
        boolean doLabel = params.containsKey(LABEL);
        boolean doMaster = params.containsKey(MASTER);
        boolean doRelease = params.containsKey(RELEASE);

        if (doMaster && !doRelease) {
            if (!doArtist) {
                cacheArtistIds();
            }
        } else if (!doMaster && doRelease) {
            if (!doArtist) {
                cacheArtistIds();
            }
            if (!doLabel) {
                cacheLabelIds();
            }
            cacheMasterIds();
        } else if (doMaster) { // doMaster && doRelease
            if (!doArtist) {
                cacheArtistIds();
            }
            if (!doLabel) {
                cacheLabelIds();
            }
        }
    }

    private void cacheMasterIds() {
        cacheThenInvert(getMasterIdentifiers(), EntityIdRegistry.Type.MASTER);
    }

    private void cacheLabelIds() {
        cacheThenInvert(getLabelIdentifiers(), EntityIdRegistry.Type.LABEL);
    }

    private void cacheArtistIds() {
        cacheThenInvert(getArtistIdentifiers(), EntityIdRegistry.Type.ARTIST);
    }

    private void cacheThenInvert(List<Long> idList, EntityIdRegistry.Type type) {
        cache(idList, type);
        invert(type);
    }

    private void invert(EntityIdRegistry.Type type) {
        log.info("inverting {} cache", type.name().toLowerCase());
        idRegistry.invert(type);
    }

    private void cache(List<Long> idList, EntityIdRegistry.Type type) {
        log.info("caching {} identifiers", type.name().toLowerCase());
        idList.stream()
                .filter(Objects::nonNull)
                .forEach(id -> idRegistry.put(type, id));
    }

    private List<Long> getArtistIdentifiers() {
        return artistRepository.findIdsBy().stream()
                .map(LongIdView::getId)
                .collect(Collectors.toList());
    }

    private List<Long> getLabelIdentifiers() {
        return labelRepository.findIdsBy().stream()
                .map(LongIdView::getId)
                .collect(Collectors.toList());
    }

    private List<Long> getMasterIdentifiers() {
        return masterRepository.findIdsBy().stream()
                .map(LongIdView::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
    }

}