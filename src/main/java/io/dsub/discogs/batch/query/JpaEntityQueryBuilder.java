package io.dsub.discogs.batch.query;

import io.dsub.discogs.batch.handler.JpaEntityHandler;

public interface JpaEntityQueryBuilder<T> extends QueryBuilder<T>, JpaEntityHandler {}
