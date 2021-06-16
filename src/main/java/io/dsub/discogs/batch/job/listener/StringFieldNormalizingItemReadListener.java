package io.dsub.discogs.batch.job.listener;

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
        .forEach(
            field -> {
              try {
                String value = ((String) field.get(item));
                if (value != null) {
                  value = value.trim();
                  if (value.isBlank()) {
                    field.set(item, null);
                  }
                }
              } catch (IllegalAccessException e) {
                log.error("failed to access field: {}", field.getName());
              }
            });
  }

  @Override
  public void onReadError(Exception ex) {
    log.error("failed to read item: {}", ex.getMessage());
  }
}
