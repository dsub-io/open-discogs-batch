package io.dsub.discogsdata.batch.job.reader;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.util.ProgressBarUtil;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.wrapped.ProgressBarWrappedInputStream;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

@Component
@RequiredArgsConstructor
public class ItemReaderBeanRegister {

  private final GenericApplicationContext ctx;

  public <T> void registerReader(DiscogsDump dump, Class<T> mappedClass) {
    SynchronizedItemStreamReader<T> reader = getReader(mappedClass, dump);
    String beanName = getReaderBeanName(mappedClass);
    // remove if already exists
    if (ctx.containsBean(beanName)) {
      ctx.removeBeanDefinition(beanName);
    }
    // register reader
    ctx.registerBean(beanName, SynchronizedItemStreamReader.class, () -> reader);
  }

  private <T> String getReaderBeanName(Class<T> mappedClass) {
    return mappedClass.getSimpleName() + "Reader";
  }

  @SuppressWarnings("unchecked")
  public <T> SynchronizedItemStreamReader<T> acquireReader(Class<T> mappedClass) {
    String beanName = getReaderBeanName(mappedClass);
    // throw if not found
    if (!ctx.containsBean(beanName) // if not contains
        || !ctx.isTypeMatch(beanName, SynchronizedItemStreamReader.class)) { // or mismatch
      throw new InvalidArgumentException("bean named " + beanName + "not found");
    }
    // safely case with type <T>
    return (SynchronizedItemStreamReader<T>) ctx.getBean(beanName);
  }

  public <T> SynchronizedItemStreamReader<T> getReader(Class<T> mappedClass, DiscogsDump dump) {
    try {
      String className = mappedClass.getSimpleName();
      StaxEventItemReader<T> reader =
          new StaxEventItemReaderBuilder<T>()
              .name(className + "Reader")
              .resource(getInputStreamResource(className, dump.getFileName(), dump.getSize()))
              .addFragmentRootElements(dump.getType().toString())
              .unmarshaller(getMarshaller(mappedClass))
              .build();
      SynchronizedItemStreamReader<T> wrapper = new SynchronizedItemStreamReader<>();
      wrapper.setDelegate(reader);
      wrapper.afterPropertiesSet();
      return wrapper;
    } catch (NoSuchFileException e) {
      throw new InitializationFailureException("failed to locate required file: " + e.getMessage());
    } catch (Exception e) {
      System.out.println("thrown class type: " + e.getClass());
      throw new InitializationFailureException(e.getMessage());
    }
  }

  private InputStreamResource getInputStreamResource(
      String className, String fileName, long fileSize) throws IOException {
    ProgressBar pb = ProgressBarUtil.get(className + " >> ", fileSize);
    InputStream in = Files.newInputStream(Path.of(fileName));
    ProgressBarWrappedInputStream pbInputStream = new ProgressBarWrappedInputStream(in, pb);
    return new InputStreamResource(new GZIPInputStream(pbInputStream));
  }

  private <T> Jaxb2Marshaller getMarshaller(Class<T> mappedClass) throws Exception {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(mappedClass);
    marshaller.afterPropertiesSet();
    return marshaller;
  }
}
