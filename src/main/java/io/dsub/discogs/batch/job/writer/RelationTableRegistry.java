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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

final class RelationTableRegistry {

  private static final String INTEGER_ARRAY_TYPE = "integer";
  private static final String TEXT_ARRAY_TYPE = "varchar";
  private static final String BYTEA_ARRAY_TYPE = "bytea";
  private static final String IDENTITY_FIELD_NAME = "identity_sha256";

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
                  List.of(ArtistNameVariation.ARTIST_NAME_VARIATION.NAME_VARIATION),
                  integerKey(ArtistNameVariation.ARTIST_NAME_VARIATION.ARTIST_ID),
                  integerKey(ArtistNameVariation.ARTIST_NAME_VARIATION.HASH)),
              table(
                  ArtistUrl.ARTIST_URL,
                  ArtistUrl.ARTIST_URL.ARTIST_ID,
                  List.of(ArtistUrl.ARTIST_URL.URL),
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
                  List.of(LabelUrl.LABEL_URL.URL),
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
                  List.of(
                      MasterVideo.MASTER_VIDEO.DESCRIPTION,
                      MasterVideo.MASTER_VIDEO.TITLE,
                      MasterVideo.MASTER_VIDEO.URL),
                  integerKey(MasterVideo.MASTER_VIDEO.MASTER_ID),
                  integerKey(MasterVideo.MASTER_VIDEO.HASH)));
      case RELEASE ->
          List.of(
              table(
                  LabelReleaseItem.LABEL_RELEASE_ITEM,
                  LabelReleaseItem.LABEL_RELEASE_ITEM.RELEASE_ITEM_ID,
                  integerKey(LabelReleaseItem.LABEL_RELEASE_ITEM.RELEASE_ITEM_ID),
                  integerKey(LabelReleaseItem.LABEL_RELEASE_ITEM.LABEL_ID),
                  textKey(LabelReleaseItem.LABEL_RELEASE_ITEM.CATEGORY_NOTATION)),
              table(
                  ReleaseItemArtist.RELEASE_ITEM_ARTIST,
                  ReleaseItemArtist.RELEASE_ITEM_ARTIST.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemArtist.RELEASE_ITEM_ARTIST.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemArtist.RELEASE_ITEM_ARTIST.ARTIST_ID)),
              table(
                  ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST,
                  ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.RELEASE_ITEM_ID,
                  List.of(ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.ROLE),
                  integerKey(
                      ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.ARTIST_ID),
                  integerKey(ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.HASH),
                  byteaKey(
                      ReleaseItemCreditedArtist.RELEASE_ITEM_CREDITED_ARTIST.IDENTITY_SHA256)),
              table(
                  ReleaseItemFormat.RELEASE_ITEM_FORMAT,
                  ReleaseItemFormat.RELEASE_ITEM_FORMAT.RELEASE_ITEM_ID,
                  List.of(
                      ReleaseItemFormat.RELEASE_ITEM_FORMAT.DESCRIPTION,
                      ReleaseItemFormat.RELEASE_ITEM_FORMAT.NAME,
                      ReleaseItemFormat.RELEASE_ITEM_FORMAT.QUANTITY,
                      ReleaseItemFormat.RELEASE_ITEM_FORMAT.QUANTITY_TEXT,
                      ReleaseItemFormat.RELEASE_ITEM_FORMAT.TEXT),
                  integerKey(ReleaseItemFormat.RELEASE_ITEM_FORMAT.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemFormat.RELEASE_ITEM_FORMAT.HASH),
                  byteaKey(ReleaseItemFormat.RELEASE_ITEM_FORMAT.IDENTITY_SHA256)),
              table(
                  ReleaseItemGenre.RELEASE_ITEM_GENRE,
                  ReleaseItemGenre.RELEASE_ITEM_GENRE.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemGenre.RELEASE_ITEM_GENRE.RELEASE_ITEM_ID),
                  textKey(ReleaseItemGenre.RELEASE_ITEM_GENRE.GENRE)),
              table(
                  ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER,
                  ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.RELEASE_ITEM_ID,
                  List.of(
                      ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.DESCRIPTION,
                      ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.TYPE,
                      ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.VALUE),
                  integerKey(ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.HASH),
                  byteaKey(ReleaseItemIdentifier.RELEASE_ITEM_IDENTIFIER.IDENTITY_SHA256)),
              table(
                  ReleaseItemStyle.RELEASE_ITEM_STYLE,
                  ReleaseItemStyle.RELEASE_ITEM_STYLE.RELEASE_ITEM_ID,
                  integerKey(ReleaseItemStyle.RELEASE_ITEM_STYLE.RELEASE_ITEM_ID),
                  textKey(ReleaseItemStyle.RELEASE_ITEM_STYLE.STYLE)),
              table(
                  ReleaseItemTrack.RELEASE_ITEM_TRACK,
                  ReleaseItemTrack.RELEASE_ITEM_TRACK.RELEASE_ITEM_ID,
                  List.of(
                      ReleaseItemTrack.RELEASE_ITEM_TRACK.DURATION,
                      ReleaseItemTrack.RELEASE_ITEM_TRACK.POSITION,
                      ReleaseItemTrack.RELEASE_ITEM_TRACK.TITLE),
                  integerKey(ReleaseItemTrack.RELEASE_ITEM_TRACK.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemTrack.RELEASE_ITEM_TRACK.HASH),
                  byteaKey(ReleaseItemTrack.RELEASE_ITEM_TRACK.IDENTITY_SHA256)),
              table(
                  ReleaseItemVideo.RELEASE_ITEM_VIDEO,
                  ReleaseItemVideo.RELEASE_ITEM_VIDEO.RELEASE_ITEM_ID,
                  List.of(
                      ReleaseItemVideo.RELEASE_ITEM_VIDEO.DESCRIPTION,
                      ReleaseItemVideo.RELEASE_ITEM_VIDEO.TITLE,
                      ReleaseItemVideo.RELEASE_ITEM_VIDEO.URL),
                  integerKey(ReleaseItemVideo.RELEASE_ITEM_VIDEO.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemVideo.RELEASE_ITEM_VIDEO.HASH),
                  byteaKey(ReleaseItemVideo.RELEASE_ITEM_VIDEO.IDENTITY_SHA256)),
              table(
                  ReleaseItemWork.RELEASE_ITEM_WORK,
                  ReleaseItemWork.RELEASE_ITEM_WORK.RELEASE_ITEM_ID,
                  List.of(ReleaseItemWork.RELEASE_ITEM_WORK.WORK),
                  integerKey(ReleaseItemWork.RELEASE_ITEM_WORK.RELEASE_ITEM_ID),
                  integerKey(ReleaseItemWork.RELEASE_ITEM_WORK.LABEL_ID),
                  integerKey(ReleaseItemWork.RELEASE_ITEM_WORK.HASH),
                  byteaKey(ReleaseItemWork.RELEASE_ITEM_WORK.IDENTITY_SHA256)));
    };
  }

  static RelationTable require(EntityType entityType, Table<?> table) {
    return forEntity(entityType).stream()
        .filter(candidate -> candidate.table().equals(table))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "table " + table.getName() + " is not a "
                        + entityType.name().toLowerCase() + " relation"));
  }

  static Optional<RelationTable> find(Table<?> table) {
    return Arrays.stream(EntityType.values())
        .flatMap(entityType -> forEntity(entityType).stream())
        .filter(candidate -> candidate.table().equals(table))
        .findFirst();
  }

  private static RelationTable table(
      Table<?> table, Field<Integer> rootIdField, RelationKey... keys) {
    return table(table, rootIdField, List.of(), keys);
  }

  private static RelationTable table(
      Table<?> table,
      Field<Integer> rootIdField,
      List<Field<?>> payloadFields,
      RelationKey... keys) {
    return new RelationTable(table, rootIdField, List.of(keys), payloadFields);
  }

  private static RelationKey integerKey(Field<Integer> field) {
    return new RelationKey(field, INTEGER_ARRAY_TYPE);
  }

  private static RelationKey textKey(Field<String> field) {
    return new RelationKey(field, TEXT_ARRAY_TYPE);
  }

  private static RelationKey byteaKey(Field<byte[]> field) {
    return new RelationKey(field, BYTEA_ARRAY_TYPE);
  }

  private RelationTableRegistry() {
  }

  record RelationTable(
      Table<?> table,
      Field<Integer> rootIdField,
      List<RelationKey> keys,
      List<Field<?>> payloadFields) {

    RelationTable {
      keys = List.copyOf(keys);
      payloadFields = List.copyOf(payloadFields);
    }

    RelationIdentity identity(UpdatableRecord<?> record) {
      return new RelationIdentity(
          table.getName(), keys.stream().map(key -> key.value(record)).toList());
    }

    List<Field<?>> conflictFields() {
      return keys.stream()
          .map(RelationKey::field)
          .filter(field -> !field.getName().equals(IDENTITY_FIELD_NAME))
          .toList();
    }

    boolean hasSamePayload(UpdatableRecord<?> left, UpdatableRecord<?> right) {
      return payloadFields.stream()
          .allMatch(field -> Objects.equals(field.getValue(left), field.getValue(right)));
    }

    void requireRoot(UpdatableRecord<?> record, int rootId) {
      if (!Objects.equals(rootIdField.getValue(record), rootId)) {
        throw new IllegalArgumentException(
            "relation " + table.getName() + " does not belong to root " + rootId);
      }
    }

    int rootId(UpdatableRecord<?> record) {
      return rootIdField.getValue(record);
    }

    String describeIdentity(UpdatableRecord<?> record) {
      return keys.stream()
          .map(key -> key.field().getName() + "=" + key.describeValue(record))
          .collect(Collectors.joining(", "));
    }

    String deleteAllForRootsSql() {
      return "delete from " + table.getName() + " where "
          + rootIdField.getName() + " = any (?)";
    }

    String existingRootsSelectSql() {
      return "select '" + table.getName() + "' as relation_table, target."
          + rootIdField.getName() + " as root_id from " + table.getName()
          + " target join incoming_roots roots on roots.root_id = target."
          + rootIdField.getName() + " group by target." + rootIdField.getName();
    }

    String deleteStaleSql(int rowCount) {
      if (rowCount < 1) {
        throw new IllegalArgumentException("stale relation row count must be positive");
      }
      String aliases =
          IntStream.range(0, keys.size())
              .mapToObj(index -> "key_" + index)
              .collect(Collectors.joining(", "));
      String rowPlaceholders =
          "(" + keys.stream().map(ignored -> "?").collect(Collectors.joining(", ")) + ")";
      String values =
          IntStream.range(0, rowCount)
              .mapToObj(ignored -> rowPlaceholders)
              .collect(Collectors.joining(", "));
      String equality =
          IntStream.range(0, keys.size())
              .mapToObj(
                  index ->
                      "target." + keys.get(index).field().getName()
                          + " is not distinct from current_keys.key_" + index)
              .collect(Collectors.joining(" and "));
      return "delete from " + table.getName() + " target where target."
          + rootIdField.getName() + " = any (?) and not exists (select 1 from (values "
          + values + ") as current_keys(" + aliases + ") where " + equality + ")";
    }
  }

  record RelationKey(Field<?> field, String postgresArrayType) {

    void bind(PreparedStatement statement, int parameterIndex, UpdatableRecord<?> record)
        throws SQLException {
      Object value = field.getValue(record);
      if (value == null) {
        statement.setNull(parameterIndex, jdbcType());
        return;
      }
      switch (postgresArrayType) {
        case INTEGER_ARRAY_TYPE -> statement.setInt(parameterIndex, (Integer) value);
        case TEXT_ARRAY_TYPE -> statement.setString(parameterIndex, (String) value);
        case BYTEA_ARRAY_TYPE -> statement.setBytes(parameterIndex, (byte[]) value);
        default -> throw new IllegalStateException(
            "unsupported relation key type " + postgresArrayType);
      }
    }

    private int jdbcType() {
      return switch (postgresArrayType) {
        case INTEGER_ARRAY_TYPE -> Types.INTEGER;
        case TEXT_ARRAY_TYPE -> Types.VARCHAR;
        case BYTEA_ARRAY_TYPE -> Types.BINARY;
        default -> throw new IllegalStateException(
            "unsupported relation key type " + postgresArrayType);
      };
    }

    RelationKeyValue value(UpdatableRecord<?> record) {
      Object value = field.getValue(record);
      return switch (postgresArrayType) {
        case INTEGER_ARRAY_TYPE -> new IntegerRelationKeyValue((Integer) value);
        case TEXT_ARRAY_TYPE -> new TextRelationKeyValue((String) value);
        case BYTEA_ARRAY_TYPE -> new ByteArrayRelationKeyValue((byte[]) value);
        default -> throw new IllegalStateException(
            "unsupported relation key type " + postgresArrayType);
      };
    }

    String describeValue(UpdatableRecord<?> record) {
      return String.valueOf(field.getValue(record));
    }
  }

  record RelationIdentity(String tableName, List<RelationKeyValue> values) {

    RelationIdentity {
      values = List.copyOf(values);
    }
  }

  sealed interface RelationKeyValue
      permits ByteArrayRelationKeyValue, IntegerRelationKeyValue, TextRelationKeyValue {
  }

  record IntegerRelationKeyValue(Integer value) implements RelationKeyValue {
  }

  record TextRelationKeyValue(String value) implements RelationKeyValue {
  }

  static final class ByteArrayRelationKeyValue implements RelationKeyValue {

    private final byte[] value;

    ByteArrayRelationKeyValue(byte[] value) {
      this.value = value == null ? null : value.clone();
    }

    byte[] value() {
      return value == null ? null : value.clone();
    }

    @Override
    public boolean equals(Object candidate) {
      return candidate instanceof ByteArrayRelationKeyValue other
          && Arrays.equals(value, other.value);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(value);
    }
  }
}
