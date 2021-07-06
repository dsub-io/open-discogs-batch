package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.jooq.tables.Artist;
import io.dsub.discogs.jooq.tables.Label;
import io.dsub.discogs.jooq.tables.Master;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

@Slf4j
@RequiredArgsConstructor
public class IdCachingJobExecutionListener implements JobExecutionListener {

  protected static final String ARTIST = "artist";
  protected static final String LABEL = "label";
  protected static final String MASTER = "master";
  protected static final String RELEASE = "release";
  protected static final String STRICT = ArgType.STRICT.getGlobalName();

  private final EntityIdRegistry idRegistry;
  private final DSLContext context;

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
        preCacheArtistIds();
      }
    } else if (!doMaster && doRelease) {
      if (!doArtist) {
        preCacheArtistIds();
      }
      if (!doLabel) {
        preCacheLabelIds();
      }
      preCacheMasterIds();
    } else if (doMaster) { // doMaster && doRelease
      if (!doArtist) {
        preCacheArtistIds();
      }
      if (!doLabel) {
        preCacheLabelIds();
      }
    }
  }

  private void preCacheMasterIds() {
    cacheThenInvert(fetchMasterIdentifiers(), EntityIdRegistry.Type.MASTER);
  }

  private void preCacheLabelIds() {
    cacheThenInvert(fetchLabelIdentifiers(), EntityIdRegistry.Type.LABEL);
  }

  private void preCacheArtistIds() {
    cacheThenInvert(fetchArtistIdentifiers(), EntityIdRegistry.Type.ARTIST);
  }

  private void cacheThenInvert(List<Integer> idList, EntityIdRegistry.Type type) {
    cache(idList, type);
    invert(type);
  }

  private void invert(EntityIdRegistry.Type type) {
    idRegistry.invert(type);
  }

  private void cache(List<Integer> idList, EntityIdRegistry.Type type) {
    log.info("caching {} identifiers", type.name().toLowerCase());
    idList.stream().filter(Objects::nonNull).forEach(id -> idRegistry.put(type, id));
  }

  private List<Integer> fetchArtistIdentifiers() {
    log.info("fetching artist identifiers");
    List<Integer> list =
        new ArrayList<>(
            context.select(Artist.ARTIST.ID).from(Artist.ARTIST).fetch(Artist.ARTIST.ID));
    log.info("fetched artists ids. count: {}", list.size());
    return list;
  }

  private List<Integer> fetchLabelIdentifiers() {
    log.info("fetching label identifiers");
    List<Integer> list =
        new ArrayList<>(context.select(Label.LABEL.ID).from(Label.LABEL).fetch(Label.LABEL.ID));
    log.info("fetched label ids. count: {}", list.size());
    return list;
  }

  private List<Integer> fetchMasterIdentifiers() {
    log.info("fetching master identifiers");
    List<Integer> list =
        new ArrayList<>(
            context.select(Master.MASTER.ID).from(Master.MASTER).fetch(Master.MASTER.ID));
    log.info("fetched master ids. count: {}", list.size());
    return list;
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
  }
}
