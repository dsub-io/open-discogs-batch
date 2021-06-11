package io.dsub.discogs.batch.domain.release;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import lombok.Data;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReleaseXML {

  ///////////////////////////////////////////////////////////////////////////
  // FIELDS
  ///////////////////////////////////////////////////////////////////////////
  @XmlAttribute(name = "id")
  private Long releaseId;

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

  @XmlElement(name = "master_id")
  private Master master;

  @XmlElement(name = "released")
  private String releaseDate;

  @XmlElementWrapper(name = "genres")
  @XmlElement(name = "genre")
  private List<String> genres;

  @XmlElementWrapper(name = "styles")
  @XmlElement(name = "style")
  private List<String> styles;

  @XmlElementWrapper(name = "artists")
  @XmlElement(name = "artist")
  private List<AlbumArtist> albumArtists;

  @XmlElementWrapper(name = "extraartists")
  @XmlElement(name = "artist")
  private List<CreditedArtist> creditedArtists;

  @XmlElementWrapper(name = "labels")
  @XmlElement(name = "label")
  private List<Label> labels;

  @XmlElementWrapper(name = "formats")
  @XmlElement(name = "format")
  private List<Format> formats;

  @XmlElementWrapper(name = "tracklist")
  @XmlElement(name = "track")
  private List<Track> tracks;

  @XmlElementWrapper(name = "identifiers")
  @XmlElement(name = "identifier")
  private List<Identifier> identifiers;

  @XmlElementWrapper(name = "companies")
  @XmlElement(name = "company")
  private List<Company> companies;

  @XmlElementWrapper(name = "videos")
  @XmlElement(name = "video")
  private List<Video> videos;

  ///////////////////////////////////////////////////////////////////////////
  // INNER CLASSES
  ///////////////////////////////////////////////////////////////////////////
  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class AlbumArtist {

    @XmlElement(name = "id")
    Long id;

    @XmlElement(name = "name")
    String name;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class CreditedArtist {

    @XmlElement(name = "id")
    Long id;

    @XmlElement(name = "name")
    String name;

    @XmlElement(name = "role")
    String role;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Label {

    @XmlAttribute(name = "catno")
    String catno;

    @XmlAttribute(name = "id")
    Long id;

    @XmlAttribute(name = "name")
    String labelName;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Format {

    @XmlAttribute(name = "name")
    String name;

    @XmlAttribute(name = "qty")
    Integer qty;

    @XmlAttribute(name = "text")
    String text;

    @XmlElementWrapper(name = "descriptions")
    @XmlElement(name = "description")
    List<String> description;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Track {

    @XmlElement(name = "position")
    String position;

    @XmlElement(name = "title")
    String title;

    @XmlElement(name = "duration")
    String duration;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Identifier {

    @XmlAttribute(name = "description")
    String description;

    @XmlAttribute(name = "type")
    String type;

    @XmlAttribute(name = "value")
    String value;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Company {

    @XmlElement(name = "id")
    Long id;

    @XmlElement(name = "entity_type_name")
    String work;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Video {

    @XmlElement(name = "title")
    String title;

    @XmlElement(name = "description")
    String description;

    @XmlAttribute(name = "src")
    String url;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Master {

    @XmlValue Long masterId;

    @XmlAttribute(name = "is_main_release")
    boolean isMaster;

    public Long getMasterId() {
      return masterId;
    }
  }
}
