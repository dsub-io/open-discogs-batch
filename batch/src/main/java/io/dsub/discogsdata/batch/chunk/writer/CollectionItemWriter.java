package io.dsub.discogsdata.batch.chunk.writer;

import lombok.Setter;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CollectionItemWriter<T> implements ItemWriter<Collection<T>>, ItemStream, InitializingBean {

    @Setter
    private ItemWriter<T> delegate;

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).open(executionContext);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).update(executionContext);
        }
    }

    @Override
    public void close() throws ItemStreamException {
        if (delegate instanceof ItemStream) {
            ((ItemStream) delegate).close();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(delegate, "delegate must not be null");
        if (delegate instanceof InitializingBean) {
            ((InitializingBean) delegate).afterPropertiesSet();
        }
    }

    @Override
    public void write(List<? extends Collection<T>> items) throws Exception {
        final List<T> consolidatedList = new ArrayList<>();

        for (final Collection<T> list : items) {
            consolidatedList.addAll(list);
        }

        while (true) {
            try {
                delegate.write(consolidatedList);
                break;
            } catch(DeadlockLoserDataAccessException ignored) {
                Thread.sleep(1000);
            }
        }
    }
}
