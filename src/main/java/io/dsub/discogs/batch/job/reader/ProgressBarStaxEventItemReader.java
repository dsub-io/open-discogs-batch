package io.dsub.discogs.batch.job.reader;

import io.dsub.discogs.batch.util.ProgressBarUtil;
import io.dsub.discogs.batch.util.ToggleProgressBarConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.wrapped.ProgressBarWrappedInputStream;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.util.Assert;

/**
 * Decorated ItemReader to show progress bar.
 *
 * @param <T> type to be read.
 */
@Slf4j
@RequiredArgsConstructor
public class ProgressBarStaxEventItemReader<T> implements ItemStreamReader<T>, InitializingBean {

  private static final String TASK_NAME_PREPEND = "READ ";

  private final String taskName;
  private final Class<T> mappedClass;
  private final Path filePath;
  private final ToggleProgressBarConsumer pbConsumer = new ToggleProgressBarConsumer(System.err);
  private String[] fragmentRootElements;
  private StaxEventItemReader<T> nestedReader;

  public ProgressBarStaxEventItemReader(
      Class<T> mappedClass, Path filePath, String... fragmentRootElements) throws Exception {
    this.mappedClass = mappedClass;
    this.filePath = filePath;
    this.taskName = TASK_NAME_PREPEND + mappedClass.getSimpleName();
    this.pbConsumer.off();
    this.fragmentRootElements =
        Arrays.stream(fragmentRootElements)
            .filter(string -> !string.isBlank())
            .toArray(String[]::new);
    this.init();
  }

  private void init() throws Exception {
    Assert.notNull(this.mappedClass, "clazz cannot be null");
    Assert.notNull(this.filePath, "filePath cannot be null");
    Assert.notEmpty(this.fragmentRootElements, "at least 1 fragmentRootElement is required");
    initDelegate();
  }

  private void initDelegate() throws Exception {
    this.nestedReader =
        new StaxEventItemReaderBuilder<T>()
            .resource(getInputStreamResource())
            .name(taskName)
            .addFragmentRootElements(fragmentRootElements)
            .unmarshaller(getUnmarshaller(mappedClass))
            .saveState(false)
            .build();
  }

  private Unmarshaller getUnmarshaller(Class<T> clazz) throws Exception {
    Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
    jaxb2Marshaller.setClassesToBeBound(clazz);
    jaxb2Marshaller.afterPropertiesSet();
    return jaxb2Marshaller;
  }

  private InputStreamResource getInputStreamResource() throws IOException {
    InputStream in = Files.newInputStream(filePath);
    ProgressBar pb = ProgressBarUtil.get(taskName, Files.size(filePath), pbConsumer);
    return new InputStreamResource(new GZIPInputStream(new ProgressBarWrappedInputStream(in, pb)));
  }

  @Override
  public synchronized T read() throws Exception {
    return nestedReader.read();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    nestedReader.afterPropertiesSet();
  }

  @Override
  public synchronized void open(ExecutionContext executionContext) throws ItemStreamException {
    this.pbConsumer.on();
    nestedReader.open(executionContext);
  }

  @Override
  public synchronized void update(ExecutionContext executionContext) throws ItemStreamException {
    nestedReader.update(executionContext);
  }

  @Override
  public void close() {
    this.pbConsumer.close();
    this.nestedReader.close();
  }
}
