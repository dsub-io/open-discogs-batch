package io.dsub.discogs.batch.job.processor;

import java.time.LocalDateTime;

/** Processes one source item using the timestamp assigned to its source chunk. */
@FunctionalInterface
public interface ObservedAtItemProcessor<I, O> {

  O process(I item, LocalDateTime observedAt) throws Exception;
}
