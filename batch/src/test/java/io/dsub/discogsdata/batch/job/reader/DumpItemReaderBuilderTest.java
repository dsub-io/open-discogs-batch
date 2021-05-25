package io.dsub.discogsdata.batch.job.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.dsub.discogsdata.batch.dto.ArtistDTO;
import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpType;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;

class DumpItemReaderBuilderTest {

  DiscogsDump dump;

  @BeforeEach
  void setUp() {
    dump = Mockito.mock(DiscogsDump.class);
  }

  @Test
  void whenBuild__ShouldNotThrow() {
    when(dump.getFileName()).thenReturn("src/test/resources/test/reader/artist.xml.gz");
    when(dump.getType()).thenReturn(DumpType.ARTIST);
    assertDoesNotThrow(() -> DumpItemReaderBuilder.build(ArtistDTO.class, dump));
  }

  @Test
  void whenTypeNotSet__ShouldThrow() {
    when(dump.getType()).thenReturn(null);
    when(dump.getFileName()).thenReturn("src/test/resources/test/reader/artist.xml.gz");
    Throwable t =
        catchThrowable(() -> DumpItemReaderBuilder.build(ArtistDTO.class, dump));
    assertThat(t)
        .hasMessageContaining("type of DiscogsDump cannot be null");
  }

  @Test
  void whenURINotSet__ShouldThrow() {
    when(dump.getFileName()).thenReturn(null);
    Throwable t =
        catchThrowable(() -> DumpItemReaderBuilder.build(ArtistDTO.class, dump));
    assertThat(t)
        .hasMessageContaining("fileName of DiscogsDump cannot be null");
  }
}