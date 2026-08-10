package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.Artist;
import io.dsub.opendiscogs.jooq.tables.Label;
import io.dsub.opendiscogs.jooq.tables.Master;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

@Slf4j
@RequiredArgsConstructor
public class IdCachingJobExecutionListener implements JobExecutionListener {

  protected static final String ARTIST = "artist";
  protected static final String LABEL = "label";
  protected static final String MASTER = "master";
  protected static final String RELEASE = "release";

  private final EntityIdRegistry idRegistry;
  private final DSLContext context;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    boolean doArtist = jobExecution.getJobParameters().getParameter(ARTIST) != null;
    boolean doLabel = jobExecution.getJobParameters().getParameter(LABEL) != null;
    boolean doMaster = jobExecution.getJobParameters().getParameter(MASTER) != null;
    boolean doRelease = jobExecution.getJobParameters().getParameter(RELEASE) != null;

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

  void preCacheMasterIds() {
    cacheIdentifiers(
        Master.MASTER.ID, Master.MASTER, DefaultEntityIdRegistry.Type.MASTER);
  }

  void preCacheLabelIds() {
    cacheIdentifiers(
        Label.LABEL.ID, Label.LABEL, DefaultEntityIdRegistry.Type.LABEL);
  }

  void preCacheArtistIds() {
    cacheIdentifiers(
        Artist.ARTIST.ID, Artist.ARTIST, DefaultEntityIdRegistry.Type.ARTIST);
  }

  private void cacheIdentifiers(
      Field<Integer> idField, Table<?> table, DefaultEntityIdRegistry.Type type) {
    log.info("caching {} identifiers", type.name().toLowerCase());
    long count =
        context.transactionResult(
            configuration -> {
              var query = DSL.using(configuration).select(idField).from(table);
              query.fetchSize(10_000);
              long cached = 0;
              try (Cursor<Record1<Integer>> cursor = query.fetchLazy()) {
                for (Record1<Integer> record : cursor) {
                  Integer id = record.value1();
                  idRegistry.put(type, id);
                  cached++;
                }
              }
              return cached;
            });
    log.info("cached {} ids. count: {}", type.name().toLowerCase(), count);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
  }
}
