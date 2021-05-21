package io.dsub.discogsdata.batch.job.reader;

import io.dsub.discogsdata.batch.util.ProgressBarUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;
import lombok.Getter;
import lombok.Setter;
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

@Slf4j
public class ProgressBarStaxEventItemReader<T> implements ItemStreamReader<T>, InitializingBean {

  private final String taskName;
  private final Class<T> clazz;
  private final Path filePath;
  private final StaxEventItemReader<T> nestedReader;
  @Getter
  @Setter
  private String taskNamePrepend = "READ ";

  public ProgressBarStaxEventItemReader(
      Class<T> clazz, Path filePath, List<String> fragmentRootElements) throws Exception {
    this.clazz = clazz;
    this.filePath = filePath;
    this.taskName = "READ " + clazz.getSimpleName();
    this.nestedReader =
        new StaxEventItemReaderBuilder<T>()
            .resource(getInputStreamResource())
            .name(taskName)
            .addFragmentRootElements(fragmentRootElements)
            .unmarshaller(getUnmarshaller(clazz))
            .saveState(false)
            .build();
    this.afterPropertiesSet();
  }

  private Unmarshaller getUnmarshaller(Class<T> clazz) throws Exception {
    Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
    jaxb2Marshaller.setClassesToBeBound(clazz);
    jaxb2Marshaller.afterPropertiesSet();
    return jaxb2Marshaller;
  }

  private InputStreamResource getInputStreamResource() throws IOException {
    InputStream in = Files.newInputStream(filePath);
    ProgressBar pb = ProgressBarUtil.get(taskName, Files.size(filePath));
    return new InputStreamResource(new ProgressBarWrappedInputStream(new GZIPInputStream(in), pb));
  }

  @Override
  public synchronized T read() throws Exception {
    return nestedReader.read();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    assert (clazz != null);
    assert (filePath != null);
    assert (nestedReader != null);
    nestedReader.afterPropertiesSet();
  }

  @Override
  public synchronized void open(ExecutionContext executionContext) throws ItemStreamException {
    nestedReader.open(executionContext);
  }

  @Override
  public synchronized void update(ExecutionContext executionContext) throws ItemStreamException {
    nestedReader.update(executionContext);
  }

  @Override
  public void close() {
    nestedReader.close();
  }
}
