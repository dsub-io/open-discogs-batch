package io.dsub.discogs.batch.job.listener;

import java.lang.reflect.Field;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ItemReadListener;

@Slf4j
public class StringFieldNormalizingItemReadListener<T> implements ItemReadListener<T> {

  /** no-op */
  @Override
  public void beforeRead() {}

  @Override
  public void afterRead(T item) {
    Arrays.stream(item.getClass().getDeclaredFields())
        .filter(field -> field.getType().equals(String.class))
        .peek(field -> field.setAccessible(true))
        .forEach(field -> normalize(item, field));
  }

  private void normalize(T item, Field stringField) {
    try {
      Object assigned = stringField.get(item);
      if (assigned == null || !assigned.toString().isBlank()) {
        return;
      }
      String s = assigned.toString();
      s = s.isBlank() ? null : s;
      stringField.set(item, s);
    } catch (Exception ignored){}
  }

  @Override
  public void onReadError(Exception ex) {
    log.error("failed to read item: {}", ex.getMessage());
  }
}
