package io.dsub.discogs.batch.job.listener;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.batch.core.ItemReadListener;

public class ListFieldDuplicationHandlingItemReadListener<T> implements ItemReadListener<T> {
  @Override
  public void beforeRead() {}

  @Override
  public void afterRead(T item) {
    Arrays.stream(item.getClass().getDeclaredFields())
        .filter(this::isListField)
        .forEach(field -> normalize(field, item));
  }

  @Override
  public void onReadError(Exception ex) {}

  private boolean isListField(Field field) {
    return field.getType().equals(List.class);
  }

  private void normalize(Field field, T item) {
    field.setAccessible(true);
    try {
      List<?> list =
          ((List<?>) field.get(item))
              .stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());
      field.set(item, list);
    } catch (Exception ignored) {
    }
  }
}
