package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.job.progress.ProcessedChunk;
import io.dsub.discogs.batch.job.progress.SourceChunk;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.transaction.support.TransactionOperations;

public class SourceChunkItemProcessor<I, O>
    implements ItemProcessor<SourceChunk<I>, ProcessedChunk<O>> {

  private final ObservedAtItemProcessor<I, O> delegate;
  private final Clock clock;
  private final TransactionOperations processingOperations;

  public SourceChunkItemProcessor(ObservedAtItemProcessor<I, O> delegate) {
    this(delegate, Clock.systemUTC(), TransactionOperations.withoutTransaction());
  }

  SourceChunkItemProcessor(ObservedAtItemProcessor<I, O> delegate, Clock clock) {
    this(delegate, clock, TransactionOperations.withoutTransaction());
  }

  public SourceChunkItemProcessor(
      ObservedAtItemProcessor<I, O> delegate, TransactionOperations processingOperations) {
    this(delegate, Clock.systemUTC(), processingOperations);
  }

  SourceChunkItemProcessor(
      ObservedAtItemProcessor<I, O> delegate,
      Clock clock,
      TransactionOperations processingOperations) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.clock = Objects.requireNonNull(clock, "clock must not be null");
    this.processingOperations =
        Objects.requireNonNull(processingOperations, "processingOperations must not be null");
  }

  @Override
  public ProcessedChunk<O> process(SourceChunk<I> source) throws Exception {
    try {
      return processingOperations.execute(ignored -> processValues(source));
    } catch (CheckedProcessingFailure failure) {
      throw failure.checkedCause();
    }
  }

  private ProcessedChunk<O> processValues(SourceChunk<I> source) {
    List<O> processed = new ArrayList<>(source.values().size());
    LocalDateTime observedAt = LocalDateTime.now(clock);
    for (I value : source.values()) {
      O result;
      try {
        result = delegate.process(value, observedAt);
      } catch (RuntimeException runtimeException) {
        throw runtimeException;
      } catch (Exception checkedException) {
        throw new CheckedProcessingFailure(checkedException);
      }
      if (result != null) {
        processed.add(result);
      }
    }
    return new ProcessedChunk<>(source.range(), processed);
  }

  private static final class CheckedProcessingFailure extends RuntimeException {

    private CheckedProcessingFailure(Exception cause) {
      super(cause);
    }

    private Exception checkedCause() {
      return (Exception) getCause();
    }
  }
}
