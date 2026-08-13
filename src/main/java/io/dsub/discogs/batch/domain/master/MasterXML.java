package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
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
  public MasterRecord buildRecord(LocalDateTime observedAt) {
    return new MasterRecord()
        .setId(id)
        .setTitle(title)
        .setYear(year)
        .setDataQuality(dataQuality)
        .setMainReleaseId(mainReleaseId)
        .setCreatedAt(observedAt)
        .setLastModifiedAt(observedAt);
  }
}
