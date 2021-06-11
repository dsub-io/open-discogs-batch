package io.dsub.discogs.batch.domain.release;

import io.dsub.discogs.batch.BatchCommand;
import io.dsub.discogs.common.entity.base.BaseEntity;
import io.dsub.discogs.common.entity.release.ReleaseItem;
import io.dsub.discogs.common.entity.release.ReleaseItemArtist;
import io.dsub.discogs.common.entity.release.ReleaseItemCreditedArtist;
import io.dsub.discogs.common.entity.release.ReleaseItemFormat;
import io.dsub.discogs.common.entity.release.ReleaseItemGenre;
import io.dsub.discogs.common.entity.release.ReleaseItemIdentifier;
import io.dsub.discogs.common.entity.release.ReleaseItemStyle;
import io.dsub.discogs.common.entity.release.ReleaseItemTrack;
import io.dsub.discogs.common.entity.release.ReleaseItemVideo;
import io.dsub.discogs.common.entity.release.ReleaseItemWork;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Batch commands for {@link io.dsub.discogsdata.common.entity.release} package. This class is NOT
 * instantiatable.
 */
public abstract class ReleaseItemBatchCommand {

  /** Private constructor to prevent instantiation. */
  private ReleaseItemBatchCommand() {}

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemCommand implements BatchCommand {

    private Long id;
    private Boolean isMaster;
    private String status;
    private String title;
    private String country;
    private String notes;
    private String dataQuality;
    private Long master;
    private Boolean hasValidMonth;
    private Boolean hasValidDay;
    private Boolean hasValidYear;
    private String listedReleaseDate;
    private LocalDate releaseDate;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItem.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemArtistCommand implements BatchCommand {

    Long releaseItem;
    Long artist;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemArtist.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemCreditedArtistCommand implements BatchCommand {

    Long releaseItem;
    Long artist;
    String role;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemCreditedArtist.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemGenreCommand implements BatchCommand {

    Long releaseItem;
    String genre;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemGenre.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemStyleCommand implements BatchCommand {

    Long releaseItem;
    String style;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemStyle.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemVideoCommand implements BatchCommand {

    Long releaseItem;
    String title;
    String description;
    String url;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemVideo.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemWorkCommand implements BatchCommand {

    Long releaseItem;
    Long label;
    String work;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemWork.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemFormatCommand implements BatchCommand {

    Long releaseItem;
    String name;
    Integer quantity;
    String text;
    String description;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemFormat.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemIdentifierCommand implements BatchCommand {

    Long releaseItem;
    String type;
    String description;
    String value;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemIdentifier.class;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class ReleaseItemTrackCommand implements BatchCommand {

    Long releaseItem;
    String position;
    String title;
    String duration;

    @Override
    public Class<? extends BaseEntity> getEntityClass() {
      return ReleaseItemTrack.class;
    }
  }
}
