package io.dsub.dumpdbmgmt.batch.processor;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ItemSetWriter<T> implements ItemWriter<Set<T>> {

    RepositoryItemWriter<T> writer;

    public ItemSetWriter(RepositoryItemWriter<T> writer) {
        this.writer = writer;
    }

    @Override
    public void write(List<? extends Set<T>> items) throws Exception {
        List<T> list = new ArrayList<>();
        items.forEach(list::addAll);
        writer.write(list);
    }
}
