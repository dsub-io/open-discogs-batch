package io.dsub.discogsdata.batch.chunk.writer;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class AsyncCollectionItemWriter<T> implements ItemStreamWriter<Future<Collection<T>>>, InitializingBean {

    @Setter
    private ItemWriter<T> delegate;

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(delegate, "A delegate ItemWriter must be provided.");
    }

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
    public void write(List<? extends Future<Collection<T>>> items) throws Exception {
        List<T> list = new ArrayList<>();
        for (Future<Collection<T>> future : items) {
            try {
                Collection<T> item = future.get();
                if (item != null && !item.isEmpty()) {
                    list.addAll(item);
                }
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    log.debug("An exception was thrown while processing an item", e);
                    throw (Exception) cause;
                } else {
                    throw e;
                }
            }
        }
        delegate.write(list);
    }
}
