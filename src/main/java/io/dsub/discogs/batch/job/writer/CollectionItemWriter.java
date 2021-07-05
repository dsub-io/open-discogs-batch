package io.dsub.discogs.batch.job.writer;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

@Slf4j
@RequiredArgsConstructor
public class CollectionItemWriter<T> implements ItemWriter<Collection<T>> {

  private final ItemWriter<T> delegate;

  @Override
  public void write(List<? extends Collection<T>> items) throws Exception {
    Map<Class<?>, List<T>> consolidatedMap = new HashMap<>();

    for (Collection<? extends T> subItems : items) {
      for (T subItem : subItems) {
        Class<?> key = subItem.getClass();
        if (!consolidatedMap.containsKey(subItem.getClass())) {
          consolidatedMap.put(key, new LinkedList<>());
        }
        consolidatedMap.get(key).add(subItem);
      }
    }

    for (List<T> subItems : consolidatedMap.values()) {
      delegate.write(subItems);
      subItems.clear();
    }
  }
}
