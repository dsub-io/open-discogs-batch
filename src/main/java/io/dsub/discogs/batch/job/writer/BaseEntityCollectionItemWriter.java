package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.common.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class BaseEntityCollectionItemWriter implements ItemWriter<Collection<BaseEntity>> {

    private final ItemWriter<BaseEntity> delegate;

    @Override
    public void write(List<? extends Collection<BaseEntity>> items) throws Exception {

        Map<Class<?>, List<BaseEntity>> consolidatedMap = new HashMap<>();

        for (Collection<? extends BaseEntity> subItems : items) {
            for (BaseEntity subItem : subItems) {
                Class<?> key = subItem.getClass();
                if (!consolidatedMap.containsKey(subItem.getClass())) {
                    consolidatedMap.put(key, new ArrayList<>());
                }
                consolidatedMap.get(key).add(subItem);
            }
        }

        for (List<BaseEntity> subItems : consolidatedMap.values()) {
            delegate.write(subItems);
        }
    }
}