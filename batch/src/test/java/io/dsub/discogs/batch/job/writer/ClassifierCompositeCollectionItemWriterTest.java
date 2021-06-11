package io.dsub.discogs.batch.job.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.item.ItemWriter;
import org.springframework.classify.Classifier;

class ClassifierCompositeCollectionItemWriterTest {

  ClassifierCompositeCollectionItemWriter<String> writer;

  @BeforeEach
  void setUp() {
    writer = new ClassifierCompositeCollectionItemWriter<>();
  }

  @Test
  void whenClassifierNotSet__ShouldThrow() {
    // when
    Throwable t = catchThrowable(() -> writer.afterPropertiesSet());

    // then
    assertThat(t).hasMessage("classifier cannot be null");
  }

  @Test
  void whenClassifierSet__ShouldNotThrow() {
    // when
    writer.setClassifier((Classifier<String, ItemWriter<? super String>>) classifiable -> null);

    // then
    assertDoesNotThrow(() -> writer.afterPropertiesSet());
  }

  @Test
  void whenWrite__ShouldCallCorrespondingWriters() {
    List<Collection<String>> sample = new ArrayList<>();
    Map<String, ItemWriter<String>> writerMap = new HashMap<>();
    OutputStream out = new ByteArrayOutputStream();
    PrintStream printStream = new PrintStream(out);

    for (char c = 'a'; c <= 'z'; c++) {
      sample.add(List.of(String.valueOf(c)));
      writerMap.put(String.valueOf(c), Mockito.spy(new TestItemWriter(printStream)));
    }

    Classifier<String, ItemWriter<? super String>> classifier = writerMap::get;
    writer.setClassifier(classifier);

    assertDoesNotThrow(() -> writer.write(sample));
    writerMap
        .values()
        .forEach(writer -> assertDoesNotThrow(() -> verify(writer, times(1)).write(any())));
  }

  static class TestItemWriter implements ItemWriter<String> {

    PrintStream printStream;

    public TestItemWriter(PrintStream printStream) {
      this.printStream = printStream;
    }

    @Override
    public void write(List<? extends String> items) {
      printStream.println(String.join("", items));
    }
  }
}
