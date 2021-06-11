package io.dsub.discogs.batch.domain.master;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class MasterXML {

  ///////////////////////////////////////////////////////////////////////////
  // FIELDS
  //////////////////////////////////////////////////////////////////////////
  @XmlAttribute(name = "id")
  private Long id;

  @XmlElement(name = "year")
  private Short year;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "data_quality")
  private String dataQuality;

  @XmlElement(name = "main_release")
  private Long mainRelease;

  @XmlElementWrapper(name = "artists")
  @XmlElement(name = "artist")
  private List<Artist> artists = new ArrayList<>();

  @XmlElementWrapper(name = "genres")
  @XmlElement(name = "genre")
  private List<String> genres = new ArrayList<>();

  @XmlElementWrapper(name = "styles")
  @XmlElement(name = "style")
  private List<String> styles = new ArrayList<>();

  @XmlElementWrapper(name = "videos")
  @XmlElement(name = "video")
  private List<Video> videos = new ArrayList<>();

  ///////////////////////////////////////////////////////////////////////////
  // INNER CLASSES
  //////////////////////////////////////////////////////////////////////////
  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Artist {

    @XmlElement(name = "id")
    private Long id;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Video {

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "description")
    private String description;

    @XmlAttribute(name = "src")
    private String url;
  }
}
