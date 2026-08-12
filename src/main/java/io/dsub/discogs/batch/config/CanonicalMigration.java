package io.dsub.discogs.batch.config;

/** One immutable canonical schema migration loaded from the model artifact. */
record CanonicalMigration(String version, String checksum, String sql) {}
