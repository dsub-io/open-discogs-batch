package io.dsub.discogs.batch.job;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.util.FileUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

class DownloadedFileCleanupUnitTest {

  @Test
  void cleanupDeletesOnlyFilesSelectedByTheManifest() throws Exception {
    FileUtil fileUtil = Mockito.mock(FileUtil.class);
    when(fileUtil.isTemporary()).thenReturn(true);
    DownloadedFileCleanup cleanup = new DownloadedFileCleanup(fileUtil);
    JobParameters parameters =
        new JobParametersBuilder()
            .addString(
                ImportJobParameters.uri(EntityType.ARTIST),
                "data/2026/discogs_20260701_artists.xml.gz")
            .addString(
                ImportJobParameters.uri(EntityType.RELEASE),
                "data/2026/discogs_20260701_releases.xml.gz")
            .toJobParameters();

    cleanup.cleanup(parameters);

    verify(fileUtil).deleteFile("discogs_20260701_artists.xml.gz");
    verify(fileUtil).deleteFile("discogs_20260701_releases.xml.gz");
    verify(fileUtil, never()).deleteFile("discogs_20260701_labels.xml.gz");
    verify(fileUtil, never()).deleteFile("discogs_20260701_masters.xml.gz");
  }
}
