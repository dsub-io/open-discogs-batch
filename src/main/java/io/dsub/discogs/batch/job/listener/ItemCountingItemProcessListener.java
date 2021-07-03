package io.dsub.discogs.batch.job.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ItemProcessListener;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class ItemCountingItemProcessListener implements ItemProcessListener<Object, Object> {

    private final AtomicLong itemsCounter;

    @Override
    public void beforeProcess(Object item) {

    }

    @Override
    public void afterProcess(Object item, Object result) {
        if (result == null) {
            return;
        }
        if (result instanceof Collection<?>){
            itemsCounter.addAndGet(((Collection<?>) result).size());
        } else {
            itemsCounter.addAndGet(1L);
        }
    }

    @Override
    public void onProcessError(Object item, Exception e) {

    }
}
