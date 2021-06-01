package io.dsub.discogsdata.batch.job.writer;

import io.dsub.discogsdata.common.exception.InitializationFailureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.classify.Classifier;

@Slf4j
@NoArgsConstructor
public class ClassifierCompositeCollectionItemWriter<T>
    implements ItemWriter<Collection<T>>, InitializingBean {

  private Classifier<T, ItemWriter<? super T>> classifier;

  public void setClassifier(Classifier<T, ItemWriter<? super T>> classifier) {
    this.classifier = classifier;
  }

  @Override
  public void write(List<? extends Collection<T>> items) throws Exception {
    Map<ItemWriter<? super T>, List<T>> consolidatedMap = new HashMap<>();
    for (Collection<T> subItems : items) {
      for (T item : subItems) {
        ItemWriter<? super T> key = classifier.classify(item);
        if (!consolidatedMap.containsKey(key)) {
          consolidatedMap.put(key, new ArrayList<>());
        }
        consolidatedMap.get(key).add(item);
      }
    }
    for (ItemWriter<? super T> itemWriter : consolidatedMap.keySet()) {
      itemWriter.write(consolidatedMap.get(itemWriter));
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (classifier == null) {
      throw new InitializationFailureException("classifier cannot be null");
    }
  }
}
