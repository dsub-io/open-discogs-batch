package io.dsub.discogs.batch.job.tasklet;

import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.common.jooq.tables.records.GenreRecord;
import io.dsub.discogs.common.jooq.tables.records.StyleRecord;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jooq.UpdatableRecord;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GenreStyleInsertionTasklet implements Tasklet {

  private final EntityIdRegistry registry;
  private final ItemWriter<UpdatableRecord<?>> jooqItemWriter;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
      throws Exception {
    contribution.setExitStatus(ExitStatus.EXECUTING);
    jooqItemWriter.write(
        registry.getStringIdSetByType(EntityIdRegistry.Type.GENRE).stream()
            .map(genre -> new GenreRecord().setName(genre))
            .collect(Collectors.toList()));
    jooqItemWriter.write(
        registry.getStringIdSetByType(EntityIdRegistry.Type.STYLE).stream()
            .map(style -> new StyleRecord().setName(style))
            .collect(Collectors.toList()));
    contribution.setExitStatus(ExitStatus.COMPLETED);
    chunkContext.setComplete();
    return RepeatStatus.FINISHED;
  }
}
