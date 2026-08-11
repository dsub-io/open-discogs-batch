package io.dsub.discogs.batch.job.progress;

@FunctionalInterface
public interface ImportProgressSink {

  void write(ImportProgressRecord record);
}
