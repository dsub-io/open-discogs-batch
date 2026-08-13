package io.dsub.discogs.batch.job.reconciliation;

/** Reconciles the denormalized Master backlink from canonical Release root state. */
public interface MasterMainReleaseReconciler {

  void reconcile();
}
