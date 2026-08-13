package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class SourceChunkItemProcessor<I, O>
    implements ItemProcessor<SourceChunk<I>, ProcessedChunk<O>> {

  private final ObservedAtItemProcessor<I, O> delegate;
  private final Clock clock;

  public SourceChunkItemProcessor(ObservedAtItemProcessor<I, O> delegate) {
    this(delegate, Clock.systemUTC());
  }

  SourceChunkItemProcessor(ObservedAtItemProcessor<I, O> delegate, Clock clock) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
  }

  @Override
  public ProcessedChunk<O> process(SourceChunk<I> source) throws Exception {
    List<O> processed = new ArrayList<>(source.values().size());
    LocalDateTime observedAt = LocalDateTime.now(clock);
    for (I value : source.values()) {
      O result = delegate.process(value, observedAt);
      if (result != null) {
        processed.add(result);
      }
    }
    return new ProcessedChunk<>(source.range(), processed);
  }
}
