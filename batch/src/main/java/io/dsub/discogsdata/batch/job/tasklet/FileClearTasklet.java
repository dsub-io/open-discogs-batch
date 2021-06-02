package io.dsub.discogsdata.batch.job.tasklet;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.exception.FileClearException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * A basic implementation of {@link Tasklet} to perform file clear. If file exists, this tasklet
 * will try to delete file from path offered by DiscogsDump.
 **/
@Slf4j
@RequiredArgsConstructor
public class FileClearTasklet implements Tasklet {

  private final DiscogsDump targetDump;

  /**
   * Deletes given file from targetDump.
   *
   * @param contribution will report {@link ExitStatus#FAILED} if failed to delete the file.
   * @param chunkContext required for implementation but will not interact.
   * @return {@link RepeatStatus#FINISHED} even if failed to delete the given file.
   */
  @Override
  public RepeatStatus execute(@NotNull StepContribution contribution,
      @NotNull ChunkContext chunkContext) {

    Path targetPath = targetDump.getResourcePath();
    try {
      if (Files.exists(targetPath)) {
        log.debug("found xml dump file: {}. deleting...", targetPath);
        Files.delete(targetPath);
        log.debug("deleted {}", targetPath);
      }
    } catch (Exception ex) {
      log.error("failed to delete file: {}.", targetPath, ex);
      contribution.setExitStatus(ExitStatus.FAILED);
      chunkContext.setComplete();
      throw new FileClearException("failed to delete file: " + targetPath);
    }
    contribution.setExitStatus(ExitStatus.COMPLETED);
    chunkContext.setComplete();
    return RepeatStatus.FINISHED;
  }
}
