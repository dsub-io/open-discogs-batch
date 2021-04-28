package io.dsub.discogsdata.batch.dump;

import org.springframework.batch.item.support.SynchronizedItemStreamReader;

public interface DumpItemReaderProvider {
    <T> SynchronizedItemStreamReader<T> getReaderFrom(Class<T> clazz, DumpItem dump, String taskName) throws Exception;
}
