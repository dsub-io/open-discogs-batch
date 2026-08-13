package io.dsub.discogs.batch.job.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.job.progress.ChunkRange;
import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdCachingItemProcessListenerUnitTest {

  @Test
  void ignoresFailedUnknownAndMissingIdResults() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    IdCachingItemProcessListener listener = new IdCachingItemProcessListener(registry);
    ArtistXML artist = new ArtistXML();

    listener.beforeProcess(artist);
    listener.afterProcess(artist, null);
    listener.afterProcess(new Object(), new Object());
    listener.afterProcess(artist, new Object());
    listener.afterProcess(new LabelXML(), new Object());
    listener.afterProcess(new MasterXML(), new Object());
    listener.afterProcess(new ReleaseItemXML(), new Object());
    listener.onProcessError(artist, new IllegalStateException("fixture"));

    verify(registry, never()).put(EntityIdRegistry.Type.ARTIST, (Integer) null);
  }

  @Test
  void cachesEveryCoreEntityAndNormalizedGenreStyleValue() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    IdCachingItemProcessListener listener = new IdCachingItemProcessListener(registry);
    ArtistXML artist = new ArtistXML();
    artist.setId(1);
    LabelXML label = new LabelXML();
    label.setId(2);
    MasterXML master = new MasterXML();
    master.setId(3);
    master.setGenres(values(null, " Rock ", " "));
    master.setStyles(List.of());
    ReleaseItemXML release = new ReleaseItemXML();
    release.setId(4);
    release.setGenres(null);
    release.setStyles(List.of(" House "));

    listener.afterProcess(artist, new Object());
    listener.afterProcess(label, new Object());
    listener.afterProcess(master, new Object());
    listener.afterProcess(release, new Object());

    verify(registry).put(EntityIdRegistry.Type.ARTIST, 1);
    verify(registry).put(EntityIdRegistry.Type.LABEL, 2);
    verify(registry).put(EntityIdRegistry.Type.MASTER, 3);
    verify(registry).put(EntityIdRegistry.Type.RELEASE, 4);
    verify(registry).put(EntityIdRegistry.Type.GENRE, "Rock");
    verify(registry).put(EntityIdRegistry.Type.STYLE, "House");
  }

  @Test
  void cachesValidItemsFromOneProcessedSourceChunk() {
    EntityIdRegistry registry = mock(EntityIdRegistry.class);
    IdCachingItemProcessListener listener = new IdCachingItemProcessListener(registry);
    ArtistXML valid = new ArtistXML();
    valid.setId(1);
    ArtistXML invalid = new ArtistXML();
    invalid.setId(0);
    ChunkRange range = new ChunkRange(0, 0, 2);

    listener.afterProcess(
        new SourceChunk<>(range, List.of(valid, invalid)),
        new ProcessedChunk<>(range, List.of(new Object())));

    verify(registry).put(EntityIdRegistry.Type.ARTIST, 1);
    verify(registry, never()).put(EntityIdRegistry.Type.ARTIST, 0);
  }

  private List<String> values(String... values) {
    return new ArrayList<>(java.util.Arrays.asList(values));
  }
}
