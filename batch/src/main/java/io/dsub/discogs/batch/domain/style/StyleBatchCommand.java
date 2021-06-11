package io.dsub.discogs.batch.domain.style;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.common.entity.Style;
import io.dsub.discogs.common.entity.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Batch command for {@link Style}. This class is NOT instantiatable. */
public abstract class StyleBatchCommand {

  /** Private constructor to prevent instantiation. */
  private StyleBatchCommand() {}

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
