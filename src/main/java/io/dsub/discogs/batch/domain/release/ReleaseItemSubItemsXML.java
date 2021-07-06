package io.dsub.discogs.batch.domain.release;

import io.dsub.discogs.batch.domain.HashXML;
import io.dsub.discogs.batch.domain.SubItemXML;
import io.dsub.discogs.jooq.tables.records.LabelReleaseItemRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemArtistRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemCreditedArtistRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemFormatRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemIdentifierRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemTrackRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemVideoRecord;
import io.dsub.discogs.jooq.tables.records.ReleaseItemWorkRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class ReleaseItemSubItemsXML {

  @XmlAttribute(name = "id")
  private Integer id;

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
  public static class ReleaseAlbumArtist implements SubItemXML<ReleaseItemArtistRecord> {

    @XmlElement(name = "id")
    Integer artistId;

    @XmlElement(name = "name")
    String name;

    @Override
    public ReleaseItemArtistRecord getRecord(int parentId) {
      return new ReleaseItemArtistRecord()
          .setArtistId(artistId)
          .setReleaseItemId(parentId)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
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
    public ReleaseItemCreditedArtistRecord getRecord(int parentId) {
      return new ReleaseItemCreditedArtistRecord()
          .setReleaseItemId(parentId)
          .setArtistId(artistId)
          .setRole(role)
          .setHash(getHashValue())
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
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
    public LabelReleaseItemRecord getRecord(int parentId) {
      return new LabelReleaseItemRecord()
          .setReleaseItemId(parentId)
          .setLabelId(labelId)
          .setCategoryNotation(categoryNotation)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ReleaseFormat implements HashXML<ReleaseItemFormatRecord> {

    @XmlAttribute(name = "name")
    String name;

    @XmlAttribute(name = "qty")
    Integer quantity;

    @XmlAttribute(name = "text")
    String text;

    @XmlElementWrapper(name = "descriptions")
    @XmlElement(name = "description")
    List<String> descriptions;

    @Override
    public int getHashValue() {
      String reducedDescription = getReducedDescription();
      return makeHash(new String[]{name, reducedDescription, text});
    }

    @Override
    public ReleaseItemFormatRecord getRecord(int parentId) {
      return new ReleaseItemFormatRecord()
          .setReleaseItemId(parentId)
          .setName(name)
          .setQuantity(quantity)
          .setText(text)
          .setDescription(getReducedDescription())
          .setHash(getHashValue())
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }

    private String getReducedDescription() {
      if (descriptions == null) {
        return null;
      }
      String description =
          descriptions.stream()
              .filter(Objects::nonNull)
              .map(String::trim)
              .filter(desc -> !desc.isBlank())
              .map(desc -> "[d:" + desc + "]")
              .collect(Collectors.joining(","));
      return description.isBlank() ? null : description;
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
    public ReleaseItemTrackRecord getRecord(int parentId) {
      return new ReleaseItemTrackRecord()
          .setReleaseItemId(parentId)
          .setPosition(position)
          .setTitle(title)
          .setDuration(duration)
          .setHash(getHashValue())
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
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
    public ReleaseItemIdentifierRecord getRecord(int parentId) {
      return new ReleaseItemIdentifierRecord()
          .setReleaseItemId(parentId)
          .setType(type)
          .setDescription(description)
          .setValue(value)
          .setHash(getHashValue())
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
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
    public ReleaseItemWorkRecord getRecord(int parentId) {
      return new ReleaseItemWorkRecord()
          .setReleaseItemId(parentId)
          .setWork(work)
          .setHash(getHashValue())
          .setLabelId(id)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
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
    public ReleaseItemVideoRecord getRecord(int parentId) {
      return new ReleaseItemVideoRecord()
          .setReleaseItemId(parentId)
          .setTitle(title)
          .setDescription(description)
          .setUrl(url)
          .setHash(getHashValue())
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
  }
}
