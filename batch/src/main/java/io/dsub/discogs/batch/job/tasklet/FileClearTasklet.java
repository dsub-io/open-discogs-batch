package io.dsub.discogs.batch.job.tasklet;

import io.dsub.discogs.batch.util.FileUtil;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * A basic implementation of {@link Tasklet} to perform file clear. If file exists, this tasklet
 * will try to delete file from path offered by DiscogsDump.
 */
@Slf4j
@RequiredArgsConstructor
public class FileClearTasklet implements Tasklet {

  private final FileUtil fileUtil;

  /**
   * Deletes given file from targetDump.
   *
   * @param contribution will report {@link ExitStatus#FAILED} if failed to delete the file.
   * @param chunkContext required for implementation but will not interact.
   * @return {@link RepeatStatus#FINISHED} even if failed to delete the given file.
   */
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    if (fileUtil.isTemporary()) {
      try {
        fileUtil.clearAll();
      } catch (IOException e) {
        log.error("failed to clear application directory.", e);
      }
    }
    contribution.setExitStatus(ExitStatus.COMPLETED);
    chunkContext.setComplete();
    return RepeatStatus.FINISHED;
  }
}
