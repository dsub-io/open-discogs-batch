package io.dsub.discogs.batch.job.listener;

import io.dsub.discogs.batch.util.ReflectionUtil;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.lang.NonNull;

public class StringNormalizingItemReadListener implements ItemReadListener<Object> {

  /* No Op */
  @Override
  public void beforeRead() {
  }

  @Override
  public void afterRead(@NonNull Object item) {
    ReflectionUtil.normalizeStringFields(item);
  }

  /* No Op */
  @Override
  public void onReadError(@NonNull Exception ex) {
  }
}
