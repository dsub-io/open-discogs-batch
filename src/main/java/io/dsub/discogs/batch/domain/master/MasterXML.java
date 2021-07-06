package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.discogs.jooq.tables.records.MasterRecord;
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
public class MasterXML implements BaseXML<MasterRecord> {

  @XmlAttribute(name = "id")
  private Integer id;

  @XmlElement(name = "year")
  private Short year;

  @XmlElement(name = "title")
  private String title;

  @XmlElement(name = "main_release")
  private Integer mainReleaseId;

  @XmlElement(name = "data_quality")
  private String dataQuality;

  @XmlElementWrapper(name = "genres")
  @XmlElement(name = "genre")
  private List<String> genres;

  @XmlElementWrapper(name = "styles")
  @XmlElement(name = "style")
  private List<String> styles;

  @Override
  public MasterRecord buildRecord() {
    return new MasterRecord()
        .setId(id)
        .setTitle(title)
        .setYear(year)
        .setDataQuality(dataQuality)
        .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
        .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
  }
}
