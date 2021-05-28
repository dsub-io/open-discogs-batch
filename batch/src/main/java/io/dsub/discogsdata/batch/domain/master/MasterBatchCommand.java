package io.dsub.discogsdata.batch.domain.master;

import io.dsub.discogsdata.batch.BatchCommand;
import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.entity.master.Master;
import io.dsub.discogsdata.common.entity.master.MasterArtist;
import io.dsub.discogsdata.common.entity.master.MasterGenre;
import io.dsub.discogsdata.common.entity.master.MasterStyle;
import io.dsub.discogsdata.common.entity.master.MasterVideo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batch commands for {@link io.dsub.discogsdata.common.entity.master} package. This class is NOT
 * instantiatable.
 */
public abstract class MasterBatchCommand {

  /**
   * Private constructor to prevent instantiation.
   */
  private MasterBatchCommand() {
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class MasterCommand implements BatchCommand {

    private Long id;
    private short year;
    private String title;
    private String dataQuality;
    private Long mainReleaseItem;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return Master.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class MasterArtistCommand implements BatchCommand {

    private Long master;
    private Long artist;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return MasterArtist.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class MasterGenreCommand implements BatchCommand {

    private Long master;
    private String genre;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return MasterGenre.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class MasterStyleCommand implements BatchCommand {

    private Long master;
    private String style;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return MasterStyle.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class MasterVideoCommand implements BatchCommand {

    private String title;
    private String description;
    private String url;
    private Long master;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return MasterVideo.class;
    }
  }
}
