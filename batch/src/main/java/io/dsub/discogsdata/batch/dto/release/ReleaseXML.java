package io.dsub.discogsdata.batch.dto.release;

import io.dsub.discogsdata.batch.aspect.annotation.XmlMapped;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import lombok.Data;

@Data
@XmlMapped
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class ReleaseXML {

  ///////////////////////////////////////////////////////////////////////////
  // FIELDS
  //////////////////////////////////////////////////////////////////////////
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

  ///////////////////////////////////////////////////////////////////////////
  // INNER CLASSES
  //////////////////////////////////////////////////////////////////////////
  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Master {

    @XmlValue
    private Long masterId;
    @XmlAttribute(name = "is_main_release")
    private boolean isMaster;
  }
}
