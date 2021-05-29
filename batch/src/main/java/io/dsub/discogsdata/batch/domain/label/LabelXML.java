package io.dsub.discogsdata.batch.domain.label;

import io.dsub.discogsdata.batch.aspect.annotation.XmlMapped;
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
@XmlMapped
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class LabelXML {

  ///////////////////////////////////////////////////////////////////////////
  // FIELDS
  //////////////////////////////////////////////////////////////////////////
  @XmlElement(name = "id")
  private Long id;

  @XmlElement(name = "name")
  private String name;

  @XmlElement(name = "contactinfo")
  private String contactInfo;

  @XmlElement(name = "profile")
  private String profile;

  @XmlElement(name = "data_quality")
  private String dataQuality;

  @XmlElementWrapper(name = "sublabels")
  @XmlElement(name = "label")
  private List<SubLabel> SubLabels;

  @XmlElementWrapper(name = "urls")
  @XmlElement(name = "url")
  private List<String> urls;

  ///////////////////////////////////////////////////////////////////////////
  // INNER CLASSES
  //////////////////////////////////////////////////////////////////////////
  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class SubLabel {

    @XmlValue
    private String name;

    @XmlAttribute(name = "id")
    private Long id;
  }
}
