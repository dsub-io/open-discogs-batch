package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.HashXML;
import io.dsub.discogs.batch.domain.SubItemXML;
import io.dsub.opendiscogs.jooq.tables.records.MasterArtistRecord;
import io.dsub.opendiscogs.jooq.tables.records.MasterVideoRecord;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
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
    public MasterArtistRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new MasterArtistRecord()
          .setMasterId(parentId)
          .setArtistId(artistId)
          .setLastModifiedAt(observedAt);
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
    public MasterVideoRecord getRecord(int parentId, LocalDateTime observedAt) {
      return new MasterVideoRecord()
          .setTitle(title)
          .setDescription(description)
          .setUrl(url)
          .setHash(getHashValue())
          .setLastModifiedAt(observedAt);
    }

    @Override
    public int getHashValue() {
      return makeHash(new String[]{title, description, url});
    }
  }
}
