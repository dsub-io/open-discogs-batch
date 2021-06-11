package io.dsub.discogs.batch.job.tasklet;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.ProgressBarUtil;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.wrapped.ProgressBarWrappedInputStream;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * A basic implementation of {@link Tasklet} to perform file fetch. If file exists, it will first
 * check the size of the file to decide whether to skip the fetch, or to delete the prior one. *
 * then fetch again. Note that each phase will trigger update to {@link
 * StepContribution#setExitStatus(ExitStatus)}.
 */
@Slf4j
@RequiredArgsConstructor
public class FileFetchTasklet implements Tasklet {

  private final DiscogsDump targetDump;
  private final FileUtil fileUtil;

  /**
   * Core implementation of {@link Tasklet#execute(StepContribution, ChunkContext)}. Will either
   * fetch and mark as success, or the opposite.
   *
   * @param contribution stepContribution to be noticed for current status.
   * @param chunkContext chunk context to clarify if repeat is necessary.
   * @return status of the task which indicates either success or fail.
   */
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

    contribution.setExitStatus(ExitStatus.EXECUTING);

    String filename = targetDump.getFileName();
    long filesize;

    try {
      filesize = fileUtil.getSize(filename);
      if (filesize > 0) {
        log.info("found duplicated file: {}. checking size...", filename);
        if (!targetDump.getSize().equals(filesize)) { // should delete then fetch again.
          log.info("incomplete size. deleting current file...");
          try {
            fileUtil.deleteFile(targetDump.getFileName());
          } catch (IOException e) {
            log.error("failed to delete incomplete file: {}", filename, e);
            chunkContext.setComplete();
            contribution.setExitStatus(ExitStatus.FAILED);
            return RepeatStatus.FINISHED;
          }
        } else {
          log.info("file already exists. proceeding...");
          chunkContext.setComplete();
          contribution.setExitStatus(ExitStatus.COMPLETED);
          return RepeatStatus.FINISHED;
        }
      }

      String message = "fetching " + targetDump.getFileName() + "...";

      try (InputStream inputStream = wrapInputStream(targetDump.getInputStream(), message)) {
        log.info(message);
        fileUtil.copy(inputStream, targetDump.getFileName());
        contribution.setExitStatus(ExitStatus.COMPLETED);
      }

      chunkContext.setComplete();
      return RepeatStatus.FINISHED;

    } catch (IOException e) {
      log.error("failed to fetch file: {}", filename, e);
      contribution.setExitStatus(ExitStatus.FAILED);
      chunkContext.setComplete();
      return RepeatStatus.FINISHED;
    }
  }

  public InputStream wrapInputStream(InputStream in, String message) {
    ProgressBar pb = ProgressBarUtil.get(message, targetDump.getSize());
    return new ProgressBarWrappedInputStream(in, pb);
  }
}
