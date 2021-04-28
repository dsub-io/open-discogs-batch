package io.dsub.discogsdata.batch.chunk.reader;

import io.dsub.discogsdata.batch.dump.DumpItem;
import io.dsub.discogsdata.batch.dump.DumpItemReaderProvider;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.stereotype.Component;

@Component
public class DefaultDumpItemReaderProvider implements DumpItemReaderProvider {
    @Override
    public <T> SynchronizedItemStreamReader<T> getReaderFrom(Class<T> clazz, DumpItem dump, String taskName) throws Exception {
        ProgressBarStaxEventItemReader<T> delegate =
                new ProgressBarStaxEventItemReader<>(clazz, dump, taskName);
        delegate.afterPropertiesSet();
        SynchronizedItemStreamReader<T> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(delegate);
        reader.afterPropertiesSet();
        return reader;
    }
}
