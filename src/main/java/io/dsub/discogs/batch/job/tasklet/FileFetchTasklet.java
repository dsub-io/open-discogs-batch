package io.dsub.discogs.batch.job.tasklet;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DiscogsDumpVerifier;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.util.FileUtil;
import io.dsub.discogs.batch.util.ProgressBarUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.wrapped.ProgressBarWrappedInputStream;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * A basic implementation of {@link Tasklet} to perform file fetch. If a file exists, it verifies
 * its SHA-256 checksum before deciding whether to reuse or replace it. Legacy dump records without
 * a checksum URL fall back to their exact object size. Note that each phase will trigger update to
 * {@link
 * StepContribution#setExitStatus(ExitStatus)}.
 */

// TODO: test!
@Slf4j
@RequiredArgsConstructor
public class FileFetchTasklet implements Tasklet {

  private final DiscogsDump targetDump;
  private final FileUtil fileUtil;
  private final DiscogsDumpVerifier dumpVerifier;

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
    if (fileExists) {
      log.info("found existing file: {}. verifying integrity...", filename);
      try {
        if (isValidFile(filename)) {
          log.info("verified file already exists. proceeding...");
          chunkContext.setComplete();
          contribution.setExitStatus(ExitStatus.COMPLETED);
          return RepeatStatus.FINISHED;
        }
      } catch (FileException e) {
        log.error(e.getMessage(), e);
        return concludeFailure(contribution, chunkContext);
      }

      log.info("file failed integrity verification. deleting current file...");
      boolean deleted = tryDeleteFile(filename);

      if (!deleted) {
        log.error("failed to delete invalid file: {}", filename);
        return concludeFailure(contribution, chunkContext);
      }
    }

    String message = "fetching " + targetDump.getFileName() + "...";

    try {
      tryCopyFile(contribution, message);
      if (!isValidFile(filename)) {
        log.error("downloaded file failed integrity verification: {}", filename);
        tryDeleteFile(filename);
        return concludeFailure(contribution, chunkContext);
      }
    } catch (FileException e) {
      log.error(e.getMessage(), e);
      return concludeFailure(contribution, chunkContext);
    }
    chunkContext.setComplete();
    return RepeatStatus.FINISHED;
  }

  private boolean isValidFile(String filename) throws FileException {
    Path file = fileUtil.getFilePath(filename);
    return dumpVerifier.isValid(targetDump, file);
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
