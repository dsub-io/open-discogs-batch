package io.dsub.discogs.batch.domain.artist;

import io.dsub.discogs.batch.domain.SubItemXML;
import io.dsub.discogs.common.jooq.tables.records.ArtistAliasRecord;
import io.dsub.discogs.common.jooq.tables.records.ArtistGroupRecord;
import io.dsub.discogs.common.jooq.tables.records.ArtistMemberRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class ArtistSubItemsXML {

  boolean prepared = false;

  @XmlElement(name = "id")
  private Integer id;

  @XmlElementWrapper(name = "aliases")
  @XmlElement(name = "name")
  private List<ArtistAliasXML> aliases;

  @XmlElementWrapper(name = "groups")
  @XmlElement(name = "name")
  private List<ArtistGroupXML> groups;

  @XmlElementWrapper(name = "members")
  @XmlElement(name = "name")
  private List<ArtistMemberXML> members;

  @XmlElementWrapper(name = "namevariations")
  @XmlElement(name = "name")
  private List<String> nameVariations;

  @XmlElementWrapper(name = "urls")
  @XmlElement(name = "url")
  private List<String> urls;

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ArtistAliasXML implements SubItemXML<ArtistAliasRecord> {

    @XmlAttribute(name = "id")
    private Integer aliasId;

    @Override
    public ArtistAliasRecord getRecord(int parentId) {
      return new ArtistAliasRecord()
          .setArtistId(parentId)
          .setAliasId(aliasId)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ArtistGroupXML implements SubItemXML<ArtistGroupRecord> {

    @XmlAttribute(name = "id")
    private Integer groupId;

    @Override
    public ArtistGroupRecord getRecord(int parentId) {
      return new ArtistGroupRecord()
          .setArtistId(parentId)
          .setGroupId(groupId)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ArtistMemberXML implements SubItemXML<ArtistMemberRecord> {

    @XmlAttribute(name = "id")
    private Integer memberId;

    @Override
    public ArtistMemberRecord getRecord(int parentId) {
      return new ArtistMemberRecord()
          .setArtistId(parentId)
          .setMemberId(memberId)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
  }
}
