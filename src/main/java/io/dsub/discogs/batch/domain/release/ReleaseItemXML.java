package io.dsub.discogs.batch.domain.release;

import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class ReleaseItemXML {

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
  private Master master;

  @XmlElementWrapper(name = "genres")
  @XmlElement(name = "genre")
  private List<String> genres;

  @XmlElementWrapper(name = "styles")
  @XmlElement(name = "style")
  private List<String> styles;

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Master {

    @XmlValue
    Integer masterId;

    @XmlAttribute(name = "is_main_release")
    boolean isMaster;
  }
}
