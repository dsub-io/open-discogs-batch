package io.dsub.discogs.batch.job.writer;

import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.opendiscogs.jooq.tables.ArtistAlias;
import io.dsub.opendiscogs.jooq.tables.ArtistGroup;
import io.dsub.opendiscogs.jooq.tables.ArtistMember;
import io.dsub.opendiscogs.jooq.tables.ArtistNameVariation;
import io.dsub.opendiscogs.jooq.tables.ArtistUrl;
import io.dsub.opendiscogs.jooq.tables.LabelReleaseItem;
import io.dsub.opendiscogs.jooq.tables.LabelSubLabel;
import io.dsub.opendiscogs.jooq.tables.LabelUrl;
import io.dsub.opendiscogs.jooq.tables.MasterArtist;
import io.dsub.opendiscogs.jooq.tables.MasterGenre;
import io.dsub.opendiscogs.jooq.tables.MasterStyle;
import io.dsub.opendiscogs.jooq.tables.MasterVideo;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemArtist;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemCreditedArtist;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemFormat;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemGenre;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemIdentifier;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemStyle;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemTrack;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemVideo;
import io.dsub.opendiscogs.jooq.tables.ReleaseItemWork;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jooq.Field;
import org.jooq.Table;

final class RelationTableRegistry {

  private static final String INTEGER_ARRAY_TYPE = "integer";
  private static final String TEXT_ARRAY_TYPE = "varchar";

  static List<RelationTable> forEntity(EntityType entityType) {
    return switch (entityType) {
      case ARTIST ->
          List.of(
              table(
                  ArtistAlias.ARTIST_ALIAS,
                  ArtistAlias.ARTIST_ALIAS.ARTIST_ID,
                  integerKey(ArtistAlias.ARTIST_ALIAS.ARTIST_ID),
                  integerKey(ArtistAlias.ARTIST_ALIAS.ALIAS_ID)),
              table(
                  ArtistGroup.ARTIST_GROUP,
                  ArtistGroup.ARTIST_GROUP.ARTIST_ID,
                  integerKey(ArtistGroup.ARTIST_GROUP.ARTIST_ID),
                  integerKey(ArtistGroup.ARTIST_GROUP.GROUP_ID)),
              table(
                  ArtistMember.ARTIST_MEMBER,
                  ArtistMember.ARTIST_MEMBER.ARTIST_ID,
                  integerKey(ArtistMember.ARTIST_MEMBER.ARTIST_ID),
                  integerKey(ArtistMember.ARTIST_MEMBER.MEMBER_ID)),
              table(
                  ArtistNameVariation.ARTIST_NAME_VARIATION,
                  ArtistNameVariation.ARTIST_NAME_VARIATION.ARTIST_ID,
                  integerKey(ArtistNameVariation.ARTIST_NAME_VARIATION.ARTIST_ID),
                  integerKey(ArtistNameVariation.ARTIST_NAME_VARIATION.HASH)),
              table(
                  ArtistUrl.ARTIST_URL,
                  ArtistUrl.ARTIST_URL.ARTIST_ID,
                  integerKey(ArtistUrl.ARTIST_URL.ARTIST_ID),
                  integerKey(ArtistUrl.ARTIST_URL.HASH)));
      case LABEL ->
          List.of(
              table(
                  LabelSubLabel.LABEL_SUB_LABEL,
                  LabelSubLabel.LABEL_SUB_LABEL.PARENT_LABEL_ID,
                  integerKey(LabelSubLabel.LABEL_SUB_LABEL.PARENT_LABEL_ID),
                  integerKey(LabelSubLabel.LABEL_SUB_LABEL.SUB_LABEL_ID)),
              table(
                  LabelUrl.LABEL_URL,
                  LabelUrl.LABEL_URL.LABEL_ID,
                  integerKey(LabelUrl.LABEL_URL.LABEL_ID),
                  integerKey(LabelUrl.LABEL_URL.HASH)));
      case MASTER ->
          List.of(
              table(
                  MasterArtist.MASTER_ARTIST,
                  MasterArtist.MASTER_ARTIST.MASTER_ID,
                  integerKey(MasterArtist.MASTER_ARTIST.MASTER_ID),
                  integerKey(MasterArtist.MASTER_ARTIST.ARTIST_ID)),
              table(
                  MasterGenre.MASTER_GENRE,
                  MasterGenre.MASTER_GENRE.MASTER_ID,
                  integerKey(MasterGenre.MASTER_GENRE.MASTER_ID),
                  textKey(MasterGenre.MASTER_GENRE.GENRE)),
              table(
                  MasterStyle.MASTER_STYLE,
                  MasterStyle.MASTER_STYLE.MASTER_ID,
                  integerKey(MasterStyle.MASTER_STYLE.MASTER_ID),
                  textKey(MasterStyle.MASTER_STYLE.STYLE)),
              table(
                  MasterVideo.MASTER_VIDEO,
                  MasterVideo.MASTER_VIDEO.MASTER_ID,
                  integerKey(MasterVideo.MASTER_VIDEO.MASTER_ID),
                  integerKey(MasterVideo.MASTER_VIDEO.HASH)));
      case RELEASE ->
          List.of(
              table(
                  LabelReleaseItem.LABEL_RELEASE_ITEM,
                  LabelReleaseItem.LABEL_RELEASE_ITEM.RELEASE_ITEM_ID,
                  integerKey(LabelReleaseItem.LABEL_RELEASE_ITEM.RELEASE_ITEM_ID),
                  integerKey(LabelReleaseItem.LABEL_RELEASE_ITEM.LABEL_ID)),
              table(
                  ReleaseItemArtist.RELEASE_ITEM_ARTIST,
                  ReleaseItemArtist.RELEASE_ITEM_ARTIST.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemArtist.RELEASE_ITEM_ARTIST.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemArtist.RELEASE_ITEM_ARTIST.ARTIST_ID)),
              table(
                  ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST,
                  ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.RELEASE_ITEM_ID,
                  integerKey(
                      ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.ARTIST_ID),
                  integerKey(ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.HASH)),
              table(
                  ReleaseItemFormat.RELEASE_ITEM_FORMAT,
                  ReleaseItemFormat.RELEASE_ITEM_FORMAT.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemFormat.RELEASE_ITEM_FORMAT.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemFormat.RELEASE_ITEM_FORMAT.HASH)),
              table(
                  ReleaseItemGenre.RELEASE_ITEM_GENRE,
                  ReleaseItemGenre.RELEASE_ITEM_GENRE.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemGenre.RELEASE_ITEM_GENRE.RELEASE_ITEM_ID),
                  textKey(ReleaseItemGenre.RELEASE_ITEM_GENRE.GENRE)),
              table(
                  ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER,
                  ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.HASH)),
              table(
                  ReleaseItemStyle.RELEASE_ITEM_STYLE,
                  ReleaseItemStyle.RELEASE_ITEM_STYLE.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemStyle.RELEASE_ITEM_STYLE.RELEASE_ITEM_ID),
                  textKey(ReleaseItemStyle.RELEASE_ITEM_STYLE.STYLE)),
              table(
                  ReleaseItemTrack.RELEASE_ITEM_TRACK,
                  ReleaseItemTrack.RELEASE_ITEM_TRACK.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemTrack.RELEASE_ITEM_TRACK.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemTrack.RELEASE_ITEM_TRACK.HASH)),
              table(
                  ReleaseItemVideo.RELEASE_ITEM_VIDEO,
                  ReleaseItemVideo.RELEASE_ITEM_VIDEO.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemVideo.RELEASE_ITEM_VIDEO.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemVideo.RELEASE_ITEM_VIDEO.HASH)),
              table(
                  ReleaseItemWork.RELEASE_ITEM_WORK,
                  ReleaseItemWork.RELEASE_ITEM_WORK.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemWork.RELEASE_ITEM_WORK.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemWork.RELEASE_ITEM_WORK.LABEL_ID),
                  integerKey(ReleaseItemWork.RELEASE_ITEM_WORK.HASH)));
    };
  }

  private static RelationTable table(
      Table<?> table, Field<Integer> rootIdField, RelationKey... keys) {
    return new RelationTable(table, rootIdField, List.of(keys));
  }

  private static RelationKey integerKey(Field<Integer> field) {
    return new RelationKey(field, INTEGER_ARRAY_TYPE);
  }

  private static RelationKey textKey(Field<String> field) {
    return new RelationKey(field, TEXT_ARRAY_TYPE);
  }

  private RelationTableRegistry() {
  }

  record RelationTable(Table<?> table, Field<Integer> rootIdField, List<RelationKey> keys) {

    String deleteAllForRootsSql() {
      return "delete from " + table.getName() + " where "
          + rootIdField.getName() + " = any (?)";
    }

    String deleteStaleSql() {
      String aliases =
          IntStream.range(0, keys.size())
              .mapToObj(index -> "key_" + index)
              .collect(Collectors.joining(", "));
      String arrays = keys.stream().map(ignored -> "?").collect(Collectors.joining(", "));
      String equality =
          IntStream.range(0, keys.size())
              .mapToObj(
                  index ->
                      "target." + keys.get(index).field().getName()
                          + " = current_keys.key_" + index)
              .collect(Collectors.joining(" and "));
      return "delete from " + table.getName() + " target where target."
          + rootIdField.getName() + " = any (?) and not exists (select 1 from unnest("
          + arrays + ") as current_keys(" + aliases + ") where " + equality + ")";
    }
  }

  record RelationKey(Field<?> field, String postgresArrayType) {
  }
}
