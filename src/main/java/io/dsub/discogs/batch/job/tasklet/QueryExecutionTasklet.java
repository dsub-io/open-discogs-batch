package io.dsub.discogs.batch.job.tasklet;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@RequiredArgsConstructor
public class QueryExecutionTasklet implements Tasklet {

  private final List<String> queries;
  private final JdbcTemplate jdbcTemplate;
  private int idx = 0;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String query = queries.get(idx++);
    log.info("executes query: {}", query);
    try {
      int affected = jdbcTemplate.update(query);
      log.info("affected {} rows", affected);
    } catch (DataAccessException e) {
      log.error(e.getMessage(), e);
      throw e;
    }

    if (idx < queries.size()) {
      return RepeatStatus.CONTINUABLE;
    }
    contribution.setExitStatus(ExitStatus.COMPLETED);
    chunkContext.setComplete();
    return RepeatStatus.FINISHED;
  }
}
