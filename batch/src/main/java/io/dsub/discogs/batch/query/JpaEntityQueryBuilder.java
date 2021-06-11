package io.dsub.discogs.batch.query;

public interface JpaEntityQueryBuilder<T> extends QueryBuilder<T>, JpaEntityExtractor<T> {}
