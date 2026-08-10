package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;

import io.dsub.discogs.batch.domain.artist.ArtistXML;
import org.junit.jupiter.api.Test;

class StringNormalizingItemReadListenerUnitTest {

  @Test
  void normalizesAfterReadAndKeepsLifecycleHooksAsNoOps() {
    StringNormalizingItemReadListener listener = new StringNormalizingItemReadListener();
    ArtistXML artist = new ArtistXML();
    artist.setName(" Artist ");

    listener.beforeRead();
    listener.afterRead(artist);
    listener.onReadError(new IllegalStateException("fixture"));

    assertThat(artist.getName()).isEqualTo("Artist");
  }
}
