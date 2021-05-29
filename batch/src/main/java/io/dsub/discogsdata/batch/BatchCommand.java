package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.common.entity.base.BaseEntity;

public interface BatchCommand {

  Class<? extends BaseEntity> getEntityClass();
}