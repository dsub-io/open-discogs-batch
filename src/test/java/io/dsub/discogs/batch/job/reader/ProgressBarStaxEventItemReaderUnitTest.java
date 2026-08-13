package io.dsub.discogs.batch.job.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.util.ToggleProgressBarConsumer;

import io.dsub.discogs.batch.TestArguments;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemReader;

class ProgressBarStaxEventItemReaderUnitTest {

  private final PrintStream stderr = System.err;
  private ByteArrayOutputStream outCaptor;

  @BeforeEach
  void setUp() {
    outCaptor = new ByteArrayOutputStream();
    System.setErr(new PrintStream(outCaptor));
  }

  @AfterEach
  void tearDown() {
    System.setErr(stderr);
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#itemReaderTestArguments")
  void whenRead__ShouldReturnValidItem(TestArguments.ItemReaderTestArgument arg) throws Exception {

    ProgressBarStaxEventItemReader<?> reader = null;

    int readCount = 0;

    try {
      reader = new ProgressBarStaxEventItemReader<>(arg.getMappedClass(), arg.getXmlPath(),
          arg.getRootElementName());
      reader.open(new ExecutionContext());
      Object item = reader.read();
      while (item != null) {
        item = reader.read();
        readCount++;
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
    }

    assertThat(readCount).isGreaterThan(0);
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#itemReaderTestArguments")
  void whenRead__ShouldPrintUpdateToSysErr(TestArguments.ItemReaderTestArgument arg)
      throws Exception {
    ProgressBarStaxEventItemReader<?> reader = null;

    try {
      reader = new ProgressBarStaxEventItemReader<>(
          arg.getMappedClass(),
          arg.getXmlPath(),
          ToggleProgressBarConsumer.interactive(System.err),
          arg.getRootElementName());
      reader.open(new ExecutionContext());
      Object item = reader.read();
      while (item != null) {
        item = reader.read();
      }
    } finally {
      if (reader != null) {
        reader.close();
      }
      assertThat(outCaptor.toString())
          .contains("SOURCE READ " + arg.getMappedClass().getSimpleName());
    }
  }

  @Test
  void whenFilePathIsNull__ShouldThrow() {
    // when
    Throwable t =
        catchThrowable(
            () -> new ProgressBarStaxEventItemReader<>(ArtistSubItemsXML.class, null, ""));

    // then
    assertThat(t)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("filePath cannot be null");
  }

  @Test
  void whenFragmentRootElementsIsNull__ShouldThrow() {
    // when
    Throwable t =
        catchThrowable(() -> new ProgressBarStaxEventItemReader<>(ArtistSubItemsXML.class,
            Path.of(TestArguments.BASE_XML_PATH, "artist.xml.gz"), ""));

    // then
    assertThat(t)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("at least 1 fragmentRootElement is required");
  }

  @Test
  @SuppressWarnings("unchecked")
  void whenOpenCalled__ShouldDelegateOpenCall() throws Exception {

    Class<ArtistSubItemsXML> mappedClass = ArtistSubItemsXML.class;
    Path path = Path.of(TestArguments.BASE_XML_PATH, "artist.xml.gz");

    ProgressBarStaxEventItemReader<ArtistSubItemsXML> reader =
        new ProgressBarStaxEventItemReader<>(mappedClass, path, "artist");

    ExecutionContext ctx = new ExecutionContext();

    Field delegateField = reader.getClass().getDeclaredField("nestedReader");
    delegateField.setAccessible(true);

    StaxEventItemReader<ArtistSubItemsXML> delegate =
        (StaxEventItemReader<ArtistSubItemsXML>) delegateField.get(reader);

    delegate = Mockito.spy(delegate);
    delegateField.set(reader, delegate);

    try {
      // when
      reader.open(ctx);

      // then
      verify(delegate, times(1)).open(ctx);

      assertThrows(ItemStreamException.class, () -> reader.open(ctx));

      verify(delegate, times(2)).open(ctx);
    } finally {
      reader.close();
    }
  }


  @Test
  @SuppressWarnings("unchecked")
  void whenAfterPropertiesSet__ShouldCallDelegate() throws Exception {
    ProgressBarStaxEventItemReader<ArtistSubItemsXML> reader = null;
    try {
      Class<ArtistSubItemsXML> mappedClass = ArtistSubItemsXML.class;
      Path path = Path.of(TestArguments.BASE_XML_PATH, "artist.xml.gz");

      reader =
          new ProgressBarStaxEventItemReader<>(mappedClass, path, "artist");

      Field delegateField = reader.getClass().getDeclaredField("nestedReader");
      delegateField.setAccessible(true);

      StaxEventItemReader<ArtistSubItemsXML> delegate =
          (StaxEventItemReader<ArtistSubItemsXML>) delegateField.get(reader);

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
