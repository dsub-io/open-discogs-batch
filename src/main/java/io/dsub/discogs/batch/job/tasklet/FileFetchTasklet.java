package io.dsub.discogs.batch.job.tasklet;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.exception.FileException;
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
    } catch (FileException e) {
      log.error(e.getMessage(), e);
      return concludeFailure(contribution, chunkContext);
    }

    boolean fileExists = filesize > 0;
    boolean filesizeMatches = filesize == targetDump.getSize();

    if (fileExists) {
      log.info("found duplicated file: {}. checking size...", filename);
      if (filesizeMatches) {
        log.info("file already exists. proceeding...");
        chunkContext.setComplete();
        contribution.setExitStatus(ExitStatus.COMPLETED);
        return RepeatStatus.FINISHED;
      }

      log.info("incomplete size. deleting current file...");
      boolean deleted = tryDeleteFile(filename);

      if (!deleted) {
        log.error("failed to delete incomplete file: {}", filename);
        return concludeFailure(contribution, chunkContext);
      }
    }

    String message = "fetching " + targetDump.getFileName() + "...";

    try {
      tryCopyFile(contribution, message);
    } catch (FileException e) {
      return concludeFailure(contribution, chunkContext);
    }
    chunkContext.setComplete();
    return RepeatStatus.FINISHED;
  }

  private void tryCopyFile(StepContribution contribution, String message) throws FileException {
    try (InputStream inputStream = wrapInputStream(targetDump.getInputStream(), message)) {
      log.info(message);
      fileUtil.copy(inputStream, targetDump.getFileName());
      contribution.setExitStatus(ExitStatus.COMPLETED);
    } catch (IOException e) {
      FileException ex = new FileException("failed to fetch " + targetDump.getFileName(), e);
      log.error(ex.getMessage(), ex);
      throw ex;
    }
  }

  private RepeatStatus concludeFailure(StepContribution contribution, ChunkContext chunkContext) {
    contribution.setExitStatus(ExitStatus.FAILED);
    chunkContext.setComplete();
    return RepeatStatus.FINISHED;
  }

  private boolean tryDeleteFile(String fileName) {
    try {
      fileUtil.deleteFile(fileName);
    } catch (FileException e) {
      log.error(e.getMessage(), e);
      return false;
    }
    return true;
  }

  public InputStream wrapInputStream(InputStream in, String message) {
    ProgressBar pb = ProgressBarUtil.get(message, targetDump.getSize());
    return new ProgressBarWrappedInputStream(in, pb);
  }
}
