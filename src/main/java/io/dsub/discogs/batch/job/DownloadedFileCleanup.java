package io.dsub.discogs.batch.job;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.exception.FileDeleteException;
import io.dsub.discogs.batch.util.FileUtil;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;

@RequiredArgsConstructor
public class DownloadedFileCleanup {

  private final FileUtil fileUtil;

  public boolean isEnabled() {
    return fileUtil.isTemporary();
  }

  public void cleanup(JobParameters parameters) throws FileDeleteException {
    for (EntityType entityType : EntityType.values()) {
      JobParameter<?> parameter = parameters.getParameter(ImportJobParameters.uri(entityType));
      if (parameter == null || parameter.value() == null) {
        continue;
      }
      String fileName = Path.of(parameter.value().toString()).getFileName().toString();
      fileUtil.deleteFile(fileName);
    }
  }
}
