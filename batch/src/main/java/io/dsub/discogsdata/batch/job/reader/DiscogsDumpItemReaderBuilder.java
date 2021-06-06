package io.dsub.discogsdata.batch.job.reader;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.util.FileUtil;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Utility class that provides single static method {@link #build(Class, DiscogsDump)}.
 */

@Component
@RequiredArgsConstructor
public class DiscogsDumpItemReaderBuilder {

  private final FileUtil fileUtil;

  public <T> SynchronizedItemStreamReader<T> build(Class<T> mappedClass, DiscogsDump dump)
      throws Exception {
    Assert.notNull(dump.getFileName(), "fileName of DiscogsDump cannot be null");
    Assert.notNull(dump.getType(), "type of DiscogsDump cannot be null");

    Path filePath = fileUtil.getFilePath(dump.getFileName());

    ProgressBarStaxEventItemReader<T> delegate;
    delegate = new ProgressBarStaxEventItemReader<>(mappedClass, filePath,
        dump.getType().toString());
    delegate.afterPropertiesSet();

    SynchronizedItemStreamReader<T> reader = new SynchronizedItemStreamReader<>();
    reader.setDelegate(delegate);
    reader.afterPropertiesSet(); // this won't trigger that of delegate's.
    return reader;
  }
}
