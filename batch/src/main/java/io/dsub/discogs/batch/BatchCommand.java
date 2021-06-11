package io.dsub.discogs.batch;

import io.dsub.discogs.common.entity.base.BaseEntity;

public interface BatchCommand {

  Class<? extends BaseEntity> getEntityClass();
}
