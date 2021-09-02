package io.dsub.discogs.batch.job.listener;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemProcessListener;

@Slf4j
@RequiredArgsConstructor
public class ItemCountingItemProcessListener implements ItemProcessListener<Object, Object> {

  private final AtomicLong itemsCounter;

  /* no op */
  @Override
  public void beforeProcess(Object item) {}

  @Override
  public void afterProcess(Object item, Object result) {
    if (result == null) {
      return;
    }
    if (Collection.class.isAssignableFrom(result.getClass())) {
      itemsCounter.addAndGet(((Collection<?>) result).size());
    } else {
      itemsCounter.addAndGet(1L);
    }
  }

  @Override
  public void onProcessError(Object item, Exception e) {
    log.error(String.format("error while processing %s >> %s", item.toString(), e.getMessage()));
  }
}
