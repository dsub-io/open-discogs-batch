package io.dsub.discogs.batch.job.listener;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import java.util.Arrays;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import io.dsub.discogs.batch.job.registry.DefaultEntityIdRegistry;
import io.dsub.opendiscogs.jooq.tables.Master;
import io.dsub.opendiscogs.jooq.tables.Artist;
import io.dsub.opendiscogs.jooq.tables.Label;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class IdCachingJobExecutionListenerUnitTest {

  @Test
  void masterOnlyCachesArtistsOnlyWhenTheyAreNotImported() {
    IdCachingJobExecutionListener listener = listener();

    listener.beforeJob(execution("master"));
    verify(listener).preCacheArtistIds();
    verify(listener, never()).preCacheLabelIds();
    verify(listener, never()).preCacheMasterIds();

    listener = listener();
    listener.beforeJob(execution("artist", "master"));
    verify(listener, never()).preCacheArtistIds();
  }

  @Test
  void releaseOnlyCachesEveryReferenceThatIsNotImported() {
    IdCachingJobExecutionListener listener = listener();
    listener.beforeJob(execution("release"));
    verify(listener).preCacheArtistIds();
    verify(listener).preCacheLabelIds();
    verify(listener).preCacheMasterIds();

    listener = listener();
    listener.beforeJob(execution("artist", "label", "release"));
    verify(listener, never()).preCacheArtistIds();
    verify(listener, never()).preCacheLabelIds();
    verify(listener).preCacheMasterIds();
  }

  @Test
  void masterAndReleaseCacheOnlyEarlierEntitiesNotInThePlan() {
    IdCachingJobExecutionListener listener = listener();
    listener.beforeJob(execution("master", "release"));
    verify(listener).preCacheArtistIds();
    verify(listener).preCacheLabelIds();
    verify(listener, never()).preCacheMasterIds();

    listener = listener();
    listener.beforeJob(execution("artist", "label", "master", "release"));
    verify(listener, never()).preCacheArtistIds();
    verify(listener, never()).preCacheLabelIds();
    verify(listener, never()).preCacheMasterIds();
  }

  @Test
  void unrelatedPlanAndAfterJobAreNoOps() {
    IdCachingJobExecutionListener listener = listener();
    JobExecution execution = execution("artist", "label");

    listener.beforeJob(execution);
    listener.afterJob(execution);

    verify(listener, never()).preCacheArtistIds();
    verify(listener, never()).preCacheLabelIds();
    verify(listener, never()).preCacheMasterIds();
  }

  @Test
  void preCacheMasterIdsStreamsEveryDatabaseIdentifier() {
    DSLContext resultContext = DSL.using(SQLDialect.POSTGRES);
    var result = resultContext.newResult(Master.MASTER.ID);
    result.add(resultContext.newRecord(Master.MASTER.ID).values(7));
    result.add(resultContext.newRecord(Master.MASTER.ID).values(9));
    MockConnection connection =
        new MockConnection(context -> new MockResult[] {new MockResult(result.size(), result)});
    DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    IdCachingJobExecutionListener listener = new IdCachingJobExecutionListener(registry, context);

    listener.preCacheMasterIds();

    verify(registry).put(DefaultEntityIdRegistry.Type.MASTER, 7);
    verify(registry).put(DefaultEntityIdRegistry.Type.MASTER, 9);
  }

  @Test
  void preCacheArtistIdsStreamsEveryDatabaseIdentifier() {
    DSLContext resultContext = DSL.using(SQLDialect.POSTGRES);
    var result = resultContext.newResult(Artist.ARTIST.ID);
    result.add(resultContext.newRecord(Artist.ARTIST.ID).values(11));
    MockConnection connection =
        new MockConnection(context -> new MockResult[] {new MockResult(result.size(), result)});
    DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    EntityIdRegistry registry = mock(EntityIdRegistry.class);

    new IdCachingJobExecutionListener(registry, context).preCacheArtistIds();

    verify(registry).put(DefaultEntityIdRegistry.Type.ARTIST, 11);
  }

  @Test
  void preCacheLabelIdsStreamsEveryDatabaseIdentifier() {
    DSLContext resultContext = DSL.using(SQLDialect.POSTGRES);
    var result = resultContext.newResult(Label.LABEL.ID);
    result.add(resultContext.newRecord(Label.LABEL.ID).values(13));
    MockConnection connection =
        new MockConnection(context -> new MockResult[] {new MockResult(result.size(), result)});
    DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    EntityIdRegistry registry = mock(EntityIdRegistry.class);

    new IdCachingJobExecutionListener(registry, context).preCacheLabelIds();

    verify(registry).put(DefaultEntityIdRegistry.Type.LABEL, 13);
  }

  private IdCachingJobExecutionListener listener() {
    IdCachingJobExecutionListener listener =
        spy(
            new IdCachingJobExecutionListener(
                mock(EntityIdRegistry.class), mock(DSLContext.class)));
    doNothing().when(listener).preCacheArtistIds();
    doNothing().when(listener).preCacheLabelIds();
    doNothing().when(listener).preCacheMasterIds();
    return listener;
  }

  private JobExecution execution(String... entities) {
    JobParametersBuilder builder = new JobParametersBuilder();
    Arrays.stream(entities).forEach(entity -> builder.addString(entity, entity));
    JobParameters parameters = builder.toJobParameters();
    return new JobExecution(1L, new JobInstance(1L, "job"), parameters);
  }
}
