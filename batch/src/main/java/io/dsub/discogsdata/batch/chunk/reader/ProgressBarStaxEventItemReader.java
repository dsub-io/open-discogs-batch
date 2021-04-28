package io.dsub.discogsdata.batch.chunk.reader;

import io.dsub.discogsdata.batch.dump.DumpItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

@Slf4j
public class ProgressBarStaxEventItemReader<T> implements ItemStreamReader<T>, InitializingBean {

    private final Class<T> clazz;
    private final DumpItem dump;
    private final StaxEventItemReader<T> nestedReader;

    public ProgressBarStaxEventItemReader(Class<T> clazz, DumpItem dump, String taskName) throws Exception {
        this.clazz = clazz;
        this.dump = dump;
        this.nestedReader = buildNestedReader(dump.getInputStream(), taskName);
        this.afterPropertiesSet();
    }

    private StaxEventItemReader<T> buildNestedReader(InputStream in, String taskName) throws Exception {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setClassesToBeBound(clazz);
        jaxb2Marshaller.afterPropertiesSet();
        return new StaxEventItemReaderBuilder<T>()
                .resource(new InputStreamResource(new GZIPInputStream(in)))
                .name(taskName)
                .addFragmentRootElements(dump.getRootElementName())
                .unmarshaller(jaxb2Marshaller)
                .build();
    }

    @Override
    public T read() throws Exception {
        return nestedReader.read();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        assert (clazz != null);
        assert (dump != null);
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
