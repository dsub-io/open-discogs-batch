package io.dsub.discogs.batch.domain.label;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.entity.label.Label;
import io.dsub.discogs.common.entity.label.LabelRelease;
import io.dsub.discogs.common.entity.label.LabelSubLabel;
import io.dsub.discogs.common.entity.label.LabelUrl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batch commands for {@link io.dsub.discogsdata.common.entity.label} package. This class is NOT
 * instantiatable.
 */
public abstract class LabelBatchCommand {

  /**
   * Private constructor to prevent instantiation.
   */
  private LabelBatchCommand() {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class LabelCommand implements BatchCommand {

    private Long id;
    private String name;
    private String contactInfo;
    private String profile;
    private String dataQuality;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return Label.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class LabelUrlCommand implements BatchCommand {

    private Long label;
    private String url;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return LabelUrl.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class LabelReleaseCommand implements BatchCommand {

    private String categoryNotation;
    private Long label;
    private Long releaseItem;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return LabelRelease.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class LabelSubLabelCommand implements BatchCommand {

    private Long parent;
    private Long subLabel;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return LabelSubLabel.class;
    }
  }
}
