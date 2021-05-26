package io.dsub.discogsdata.batch.job.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dsub.discogsdata.batch.domain.artist.ArtistXML;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.xml.StaxEventItemReader;

class ProgressBarStaxEventItemReaderTest {

  private final PrintStream stdout = System.out;
  private ByteArrayOutputStream outCaptor;
  private final String basePathStr = "src/test/resources/test/reader";

  private final Path artistPath = Path.of(basePathStr, "artist.xml.gz");
  private final Path labelPath = Path.of(basePathStr, "label.xml.gz");
  private final Path masterPath = Path.of(basePathStr, "master.xml.gz");
  private final Path releasePath = Path.of(basePathStr, "release.xml.gz");

  @BeforeEach
  void setUp() {
    outCaptor = new ByteArrayOutputStream();
    System.setErr(new PrintStream(outCaptor));
  }

  @Test
  void whenRead__ShouldReturnValidItem() throws Exception {
    ProgressBarStaxEventItemReader<ArtistXML> reader = null;
    try {
      reader = new ProgressBarStaxEventItemReader<>(ArtistXML.class, artistPath, "artist");
      reader.open(new ExecutionContext());
      ArtistXML item = reader.read();
      while (item != null) {
        assertThat(item).isNotNull();
        assertThat(item.getId()).isNotNull();
        item = reader.read();
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  @Test
  void whenRead__ShouldPrintUpdateToSysErr() {
    ProgressBarStaxEventItemReader<ArtistXML> reader = null;
    try {
      reader = new ProgressBarStaxEventItemReader<>(ArtistXML.class, artistPath, "artist");
      reader.open(new ExecutionContext());
      ArtistXML item = reader.read();
      while (item != null) {
        item = reader.read();
      }
    } catch (Exception e) {
      fail(e);
    } finally {
      if (reader != null) {
        reader.close();
      }
      assertThat(outCaptor.toString()).contains("READ ArtistXML 100%");
    }
  }

  @Test
  void whenFilePathIsNull__ShouldThrow() {
    // when
    Throwable t = catchThrowable(
        () -> new ProgressBarStaxEventItemReader<>(ArtistXML.class, null, ""));

    // then
    assertThat(t).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("filePath cannot be null");
  }

  @Test
  void whenFragmentRootElementsIsNull__ShouldThrow() {
    // when
    Throwable t = catchThrowable(() ->
        new ProgressBarStaxEventItemReader<>(ArtistXML.class, artistPath, ""));

    // then
    assertThat(t).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("at least 1 fragmentRootElement is required");
  }

  @Test
  @SuppressWarnings("unchecked")
  void whenCallUpdate__ShouldDelegate() throws Exception {
    ProgressBarStaxEventItemReader<ArtistXML> reader =
        new ProgressBarStaxEventItemReader<>(ArtistXML.class, artistPath, "artist");

    ExecutionContext ctx = new ExecutionContext();

    Field delegateField = reader.getClass().getDeclaredField("nestedReader");
    delegateField.setAccessible(true);

    StaxEventItemReader<ArtistXML> delegate =
        (StaxEventItemReader<ArtistXML>) delegateField.get(reader);

    delegate = Mockito.spy(delegate);
    delegateField.set(reader, delegate);

    // when
    reader.open(ctx);

    // then
    verify(delegate, times(1)).open(ctx);

    assertThrows(ItemStreamException.class, () -> reader.open(ctx));

    verify(delegate, times(2)).open(ctx);
  }

  @Test
  @SuppressWarnings("unchecked")
  void whenAfterPropertiesSet__ShouldCallDelegate() throws Exception {
    ProgressBarStaxEventItemReader<ArtistXML> reader = null;
    try {
      reader = new ProgressBarStaxEventItemReader<>(ArtistXML.class, artistPath, "artist");

      Field delegateField = reader.getClass().getDeclaredField("nestedReader");
      delegateField.setAccessible(true);

      StaxEventItemReader<ArtistXML> delegate =
          (StaxEventItemReader<ArtistXML>) delegateField.get(reader);

      delegate = Mockito.spy(delegate);
      delegateField.set(reader, delegate);

      // when
      reader.afterPropertiesSet();

      // then
      verify(delegate, times(1)).afterPropertiesSet();
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }
}