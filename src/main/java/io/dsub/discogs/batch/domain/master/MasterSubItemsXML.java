package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.HashXML;
import io.dsub.discogs.batch.domain.SubItemXML;
import io.dsub.discogs.common.jooq.tables.records.MasterArtistRecord;
import io.dsub.discogs.common.jooq.tables.records.MasterVideoRecord;
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
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class MasterSubItemsXML {

  @XmlAttribute(name = "id")
  private Integer id;

  @XmlElementWrapper(name = "artists")
  @XmlElement(name = "artist")
  private List<MasterArtistXML> masterArtists;

  @XmlElementWrapper(name = "genres")
  @XmlElement(name = "genre")
  private List<String> genres;

  @XmlElementWrapper(name = "styles")
  @XmlElement(name = "style")
  private List<String> styles;

  @XmlElementWrapper(name = "videos")
  @XmlElement(name = "video")
  private List<MasterVideoXML> masterVideos;

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class MasterArtistXML implements SubItemXML<MasterArtistRecord> {

    @XmlElement(name = "id")
    private Integer artistId;

    @Override
    public MasterArtistRecord getRecord(int parentId) {
      return new MasterArtistRecord()
          .setMasterId(parentId)
          .setArtistId(artistId)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class MasterVideoXML implements HashXML<MasterVideoRecord> {

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "description")
    private String description;

    @XmlAttribute(name = "src")
    private String url;

    @Override
    public MasterVideoRecord getRecord(int parentId) {
      return new MasterVideoRecord()
          .setTitle(title)
          .setDescription(description)
          .setUrl(url)
          .setHash(getHashValue())
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }

    @Override
    public int getHashValue() {
      return makeHash(new String[]{title, description, url});
    }
  }
}
