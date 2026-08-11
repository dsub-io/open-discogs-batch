package io.dsub.discogs.batch.job.progress;

public enum ImportProgressState {
  STARTED,
  RUNNING,
  COMPLETED,
  FAILED,
  OBSERVATION_ERROR
}
