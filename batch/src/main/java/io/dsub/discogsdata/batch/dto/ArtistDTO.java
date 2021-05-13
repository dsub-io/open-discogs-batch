package io.dsub.discogsdata.batch.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlMapped
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArtistDTO {
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
}
