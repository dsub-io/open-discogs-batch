package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class SourceChunkItemProcessor<I, O>
    implements ItemProcessor<SourceChunk<I>, ProcessedChunk<O>> {

  private final ItemProcessor<I, O> delegate;

  public SourceChunkItemProcessor(ItemProcessor<I, O> delegate) {
    this.delegate = delegate;
  }

  @Override
  public ProcessedChunk<O> process(SourceChunk<I> source) throws Exception {
    List<O> processed = new ArrayList<>(source.values().size());
    for (I value : source.values()) {
      O result = delegate.process(value);
      if (result != null) {
        processed.add(result);
      }
    }
    return new ProcessedChunk<>(source.range(), processed);
  }
}
