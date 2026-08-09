package io.dsub.discogs.batch.job.writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

@Slf4j
@RequiredArgsConstructor
public class CollectionItemWriter<T> implements ItemWriter<Collection<T>> {

  private final ItemWriter<T> delegate;
  private final int maxBatchSize;

  @Override
  public void write(Chunk<? extends Collection<T>> items) throws Exception {
    Map<Class<?>, List<T>> consolidatedMap = new HashMap<>();

    for (Collection<? extends T> subItems : items) {
      for (T subItem : subItems) {
        Class<?> key = subItem.getClass();
        List<T> batch =
            consolidatedMap.computeIfAbsent(key, ignored -> new ArrayList<>(maxBatchSize));
        batch.add(subItem);
        if (batch.size() == maxBatchSize) {
          delegate.write(new Chunk<>(batch));
          batch.clear();
        }
      }
    }

    for (List<T> subItems : consolidatedMap.values()) {
      if (!subItems.isEmpty()) {
        delegate.write(new Chunk<>(subItems));
      }
    }
  }
}
