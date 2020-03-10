package io.dsub.dumpdbmgmt.batch;

import lombok.Getter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;

/**
 * ItemReader which wraps a standard StaxEventItemReader
 * with synchronized read function.
 *
 * @param <T> type of entity.
 */

@Getter
public class CustomStaxEventItemReader<T> implements ItemReader<T>, ItemStream, InitializingBean, ResourceAwareItemReaderItemStream<T> {

    private final StaxEventItemReader<T> nestedReader;

    public CustomStaxEventItemReader(StaxEventItemReader<T> reader) {
        this.nestedReader = reader;
    }

    @Override
    public synchronized T read() throws Exception {
        return nestedReader.read();
    }

    @Override
    public void setResource(Resource resource) {
        nestedReader.setResource(resource);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        nestedReader.afterPropertiesSet();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        nestedReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        nestedReader.update(executionContext);
    }

    @Override
    public void close() {
        nestedReader.close();
    }

    public boolean isSaveState() {
        return nestedReader.isSaveState();
    }

    public void setSaveState(boolean saveState) {
        nestedReader.setSaveState(saveState);
    }

    public void setStrict(boolean strict) {
        nestedReader.setStrict(strict);
    }

    public void setCurrentItemCount(int count) {
        nestedReader.setCurrentItemCount(count);
    }

    public void setFragmentRootElementName(String fragmentRootElementName) {
        nestedReader.setFragmentRootElementName(fragmentRootElementName);
    }

    public void setMaxItemCount(int count) {
        nestedReader.setMaxItemCount(count);
    }

    public void setUnmarshaller(Unmarshaller unmarshaller) {
        nestedReader.setUnmarshaller(unmarshaller);
    }

    public void setName(String name) {
        nestedReader.setName(name);
    }
}
