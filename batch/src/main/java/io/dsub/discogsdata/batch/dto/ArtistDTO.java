package io.dsub.discogsdata.batch.dto;

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
@XmlMapped
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArtistDTO {

  ///////////////////////////////////////////////////////////////////////////
  // FIELDS
  ///////////////////////////////////////////////////////////////////////////
  @XmlElement(name = "id")
  private Long id;
  @XmlElement(name = "name")
  private String name;
  @XmlElement(name = "realname")
  private String realName;
  @XmlElement(name = "profile")
  private String profile;
  @XmlElement(name = "data_quality")
  private String dataQuality;
  @XmlElementWrapper(name = "aliases")
  @XmlElement(name = "name")
  private List<Alias> aliases;
  @XmlElementWrapper(name = "groups")
  @XmlElement(name = "name")
  private List<Group> groups;
  @XmlElementWrapper(name = "members")
  @XmlElement(name = "name")
  private List<Member> members;
  @XmlElementWrapper(name = "namevariations")
  @XmlElement(name = "name")
  private List<String> nameVariations;
  @XmlElementWrapper(name = "urls")
  @XmlElement(name = "url")
  private List<String> urls;

  ///////////////////////////////////////////////////////////////////////////
  // INNER CLASSES
  //////////////////////////////////////////////////////////////////////////
  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Alias {

    @XmlAttribute(name = "id")
    private Long id;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Group {

    @XmlAttribute(name = "id")
    private Long id;
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Member {

    @XmlAttribute(name = "id")
    private Long id;
  }
}
