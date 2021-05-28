package io.dsub.discogsdata.batch.domain.style;

import io.dsub.discogsdata.batch.BatchCommand;
import io.dsub.discogsdata.common.entity.Style;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batch command for {@link io.dsub.discogsdata.common.entity.Style}. This class is NOT
 * instantiatable.
 */
public abstract class StyleBatchCommand {

  /**
   * Private constructor to prevent instantiation.
   */
  private StyleBatchCommand() {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class StyleCommand implements BatchCommand {

    private String name;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return Style.class;
    }
  }
}
