package io.dsub.discogs.batch.domain.release;

import io.dsub.discogs.batch.domain.HashXML;
import io.dsub.discogs.batch.domain.SubItemXML;
import io.dsub.discogs.batch.util.DiscogsStringNormalizer;
import io.dsub.opendiscogs.jooq.tables.records.LabelReleaseItemRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemCreditedArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemFormatRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemIdentifierRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemTrackRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemVideoRecord;
import io.dsub.opendiscogs.jooq.tables.records.ReleaseItemWorkRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class ReleaseItemSubItemsXML {

  @XmlAttribute(name = "id")
  private Integer id;

  @XmlAttribute(name = "status")
  private String status;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "country")
  private String country;

  @XmlElement(name = "notes")
  private String notes;

  @XmlElement(name = "data_quality")
  private String dataQuality;

  @XmlElement(name = "released")
  private String releaseDate;

  @XmlElement(name = "master_id")
  private ReleaseMaster master;

  @XmlElementWrapper(name = "artists")
  @XmlElement(name = "artist")
  private List<ReleaseAlbumArtist> releaseAlbumArtists;

  @XmlElementWrapper(name = "extraartists")
  @XmlElement(name = "artist")
  private List<ReleaseCreditedArtist> releaseCreditedArtists;

  @XmlElementWrapper(name = "labels")
  @XmlElement(name = "label")
  private List<LabelItemRelease> labelReleaseLabels;

  @XmlElementWrapper(name = "formats")
  @XmlElement(name = "format")
  private List<ReleaseFormat> releaseFormats;

  @XmlElementWrapper(name = "tracklist")
  @XmlElement(name = "track")
  private List<ReleaseTrack> releaseTracks;

  @XmlElementWrapper(name = "identifiers")
  @XmlElement(name = "identifier")
  private List<ReleaseIdentifier> releaseIdentifiers;

  @XmlElementWrapper(name = "companies")
  @XmlElement(name = "company")
  private List<ReleaseWork> companies;

  @XmlElementWrapper(name = "videos")
  @XmlElement(name = "video")
  private List<ReleaseVideo> releaseVideos;

  @XmlElementWrapper(name = "genres")
  @XmlElement(name = "genre")
  private List<String> genres;

  @XmlElementWrapper(name = "styles")
  @XmlElement(name = "style")
  private List<String> styles;

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseMaster {

    @jakarta.xml.bind.annotation.XmlValue
    Integer masterId;

    @XmlAttribute(name = "is_main_release")
    boolean mainRelease;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseAlbumArtist implements SubItemXML<ReleaseItemArtistRecord> {

    @XmlElement(name = "id")
    Integer artistId;

    @XmlElement(name = "name")
    String name;

    @Override
    public ReleaseItemArtistRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new ReleaseItemArtistRecord()
          .setArtistId(artistId)
          .setReleaseItemId(parentId)
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseCreditedArtist implements HashXML<ReleaseItemCreditedArtistRecord> {

    @XmlElement(name = "id")
    Integer artistId;

    @XmlElement(name = "name")
    String name;

    @XmlElement(name = "role")
    String role;

    @Override
    public int getHashValue() {
      return role == null || role.isBlank() ? this.hashCode() : role.hashCode();
    }

    @Override
    public ReleaseItemCreditedArtistRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new ReleaseItemCreditedArtistRecord()
          .setReleaseItemId(parentId)
          .setArtistId(artistId)
          .setRole(role)
          .setHash(getHashValue())
          .setIdentitySha256(
              ReleaseRelationIdentity.digest(
                  ReleaseRelationIdentity.Relation.CREDITED_ARTIST, role))
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class LabelItemRelease implements SubItemXML<LabelReleaseItemRecord> {

    @XmlAttribute(name = "catno")
    String categoryNotation;

    @XmlAttribute(name = "id")
    Integer labelId;

    @XmlAttribute(name = "name")
    String labelName;

    @Override
    public LabelReleaseItemRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new LabelReleaseItemRecord()
          .setReleaseItemId(parentId)
          .setLabelId(labelId)
          .setCategoryNotation(categoryNotation)
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseFormat implements HashXML<ReleaseItemFormatRecord> {

    private static final String HASH_FIELD_SEPARATOR = "\0";
    private static final String HASH_NULL_VALUE = "\1";
    private static final String MAX_INT_32_DECIMAL = "2147483647";

    @XmlAttribute(name = "name")
    String name;

    @XmlAttribute(name = "qty")
    String quantity;

    @XmlAttribute(name = "text")
    String text;

    @XmlElementWrapper(name = "descriptions")
    @XmlElement(name = "description")
    List<String> descriptions;

    @Override
    public int getHashValue() {
      return hashValue(getReducedDescription(), canonicalQuantity());
    }

    private int hashValue(String reducedDescription, String canonicalQuantity) {
      return String.join(
              HASH_FIELD_SEPARATOR,
              hashString(name),
              hashString(reducedDescription),
              hashString(canonicalQuantity),
              hashString(text))
          .hashCode();
    }

    @Override
    public ReleaseItemFormatRecord getRecord(int parentId, LocalDateTime observedAt) {
      String canonicalQuantity = canonicalQuantity();
      String reducedDescription = getReducedDescription();
      return new ReleaseItemFormatRecord()
          .setReleaseItemId(parentId)
          .setName(name)
          .setQuantity(integerQuantity(canonicalQuantity))
          .setQuantityText(canonicalQuantity)
          .setText(text)
          .setDescription(reducedDescription)
          .setHash(hashValue(reducedDescription, canonicalQuantity))
          .setIdentitySha256(
              ReleaseRelationIdentity.digest(
                  ReleaseRelationIdentity.Relation.FORMAT,
                  name,
                  reducedDescription,
                  canonicalQuantity,
                  text))
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }

    private String getReducedDescription() {
      if (descriptions == null) {
        return null;
      }
      String description =
          descriptions.stream()
              .filter(Objects::nonNull)
              .map(DiscogsStringNormalizer::normalizeNullable)
              .filter(Objects::nonNull)
              .map(desc -> "[d:" + desc + "]")
              .collect(Collectors.joining(","));
      return description.isBlank() ? null : description;
    }

    private String hashString(String value) {
      return value == null ? HASH_NULL_VALUE : value;
    }

    private String canonicalQuantity() {
      String normalized = DiscogsStringNormalizer.normalizeNullable(quantity);
      if (normalized == null) {
        return null;
      }
      for (int index = 0; index < normalized.length(); index++) {
        char value = normalized.charAt(index);
        if (value < '0' || value > '9') {
          throw new IllegalArgumentException("invalid non-negative release format quantity");
        }
      }
      int firstSignificant = 0;
      while (firstSignificant < normalized.length() - 1
          && normalized.charAt(firstSignificant) == '0') {
        firstSignificant++;
      }
      return normalized.substring(firstSignificant);
    }

    private Integer integerQuantity(String canonical) {
      if (canonical == null) {
        return null;
      }
      if (canonical.length() > MAX_INT_32_DECIMAL.length()
          || (canonical.length() == MAX_INT_32_DECIMAL.length()
              && canonical.compareTo(MAX_INT_32_DECIMAL) > 0)) {
        return null;
      }
      return Integer.valueOf(canonical);
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseTrack implements HashXML<ReleaseItemTrackRecord> {

    @XmlElement(name = "position")
    String position;

    @XmlElement(name = "title")
    String title;

    @XmlElement(name = "duration")
    String duration;

    @Override
    public int getHashValue() {
      return makeHash(new String[]{position, title, duration});
    }

    @Override
    public ReleaseItemTrackRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new ReleaseItemTrackRecord()
          .setReleaseItemId(parentId)
          .setPosition(position)
          .setTitle(title)
          .setDuration(duration)
          .setHash(getHashValue())
          .setIdentitySha256(
              ReleaseRelationIdentity.digest(
                  ReleaseRelationIdentity.Relation.TRACK, position, title, duration))
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseIdentifier implements HashXML<ReleaseItemIdentifierRecord> {

    @XmlAttribute(name = "type")
    String type;

    @XmlAttribute(name = "description")
    String description;

    @XmlAttribute(name = "value")
    String value;

    @Override
    public int getHashValue() {
      return makeHash(new String[]{type, description, value});
    }

    @Override
    public ReleaseItemIdentifierRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new ReleaseItemIdentifierRecord()
          .setReleaseItemId(parentId)
          .setType(type)
          .setDescription(description)
          .setValue(value)
          .setHash(getHashValue())
          .setIdentitySha256(
              ReleaseRelationIdentity.digest(
                  ReleaseRelationIdentity.Relation.IDENTIFIER, type, description, value))
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseWork implements HashXML<ReleaseItemWorkRecord> {

    @XmlElement(name = "id")
    Integer id;

    @XmlElement(name = "entity_type_name")
    String work;

    @Override
    public int getHashValue() {
      return work == null || work.isBlank() ? this.hashCode() : work.hashCode();
    }

    @Override
    public ReleaseItemWorkRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new ReleaseItemWorkRecord()
          .setReleaseItemId(parentId)
          .setWork(work)
          .setHash(getHashValue())
          .setIdentitySha256(
              ReleaseRelationIdentity.digest(
                  ReleaseRelationIdentity.Relation.WORK, work))
          .setLabelId(id)
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseVideo implements HashXML<ReleaseItemVideoRecord> {

    @XmlElement(name = "title")
    String title;

    @XmlElement(name = "description")
    String description;

    @XmlAttribute(name = "src")
    String url;

    @Override
    public int getHashValue() {
      return makeHash(new String[]{title, description, url});
    }

    @Override
    public ReleaseItemVideoRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new ReleaseItemVideoRecord()
          .setReleaseItemId(parentId)
          .setTitle(title)
          .setDescription(description)
          .setUrl(url)
          .setHash(getHashValue())
          .setIdentitySha256(
              ReleaseRelationIdentity.digest(
                  ReleaseRelationIdentity.Relation.VIDEO, title, description, url))
          .setCreatedAt(observedAt)
          .setLastModifiedAt(observedAt);
    }
  }
}
