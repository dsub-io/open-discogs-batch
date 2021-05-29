package io.dsub.discogsdata.batch.job.tasklet;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.exception.FileFetchException;
import io.dsub.discogsdata.batch.util.ProgressBarUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
    Path targetPath = Path.of(targetDump.getFileName());

    if (Files.exists(targetPath)) {
      log.info("found duplicated file: " + targetPath + ". checking size...");
      boolean sizeMatched = checkFileSize(targetDump.getSize(), targetPath);
      if (!sizeMatched) { // should delete then fetch again.
        log.info("incomplete size. deleting current file...");
        contribution.setExitStatus(ExitStatus.EXECUTING);
        try {
          deleteFile(targetPath);
        } catch (FileFetchException e) {
          chunkContext.setComplete(); // to clarify state of failure
          contribution.setExitStatus(ExitStatus.FAILED);
          throw e;
        }
      } else {
        log.info("file already exists. proceeding..."); // file is already intact
        chunkContext.setComplete();
        contribution.setExitStatus(ExitStatus.COMPLETED);
        return RepeatStatus.FINISHED;
      }
    }

    try {
      log.info("fetching " + targetPath + "...");
      InputStream in = targetDump.getUrl().openStream();
      String taskName = "fetching " + targetDump.getFileName() + "...";
      ProgressBar pb = ProgressBarUtil.get(taskName, targetDump.getSize());
      ProgressBarWrappedInputStream wrappedIn = new ProgressBarWrappedInputStream(in, pb);
      Files.copy(wrappedIn, targetPath);
    } catch (IOException e) {
      throw new FileFetchException(
          "failed on copying " + targetDump.getETag() + ". reason: " + e.getMessage());
    }
    chunkContext.setComplete();
    contribution.setExitStatus(ExitStatus.COMPLETED);
    return RepeatStatus.FINISHED; // or else failed...2
  }

  /**
   * Check if file of the given path matches the expected size. If file does not exists, then it
   * should return negative.
   *
   * @param expectedSize expected file size
   * @param path         path where file is expected to be exists
   * @return whether the file size is same as expected, or false if the file not exists.
   */
  public boolean checkFileSize(long expectedSize, Path path) {
    try {
      return Files.size(path) == expectedSize;
    } catch (Exception e) {
      throw new FileFetchException(
          "failed to check file size: " + path + ". reason: " + e.getMessage());
    }
  }

  /**
   * Deletes a file from given path if exists.
   *
   * @param filePath path of file to be deleted.
   */
  protected void deleteFile(Path filePath) {
    try {
      Files.deleteIfExists(filePath);
    } catch (Exception e) {
      throw new FileFetchException(
          "failed to delete file: " + filePath + ". reason: " + e.getMessage());
    }
  }
}
