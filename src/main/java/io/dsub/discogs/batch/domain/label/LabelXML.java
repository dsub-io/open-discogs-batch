package io.dsub.discogs.batch.domain.label;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.opendiscogs.jooq.tables.records.LabelRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class LabelXML implements BaseXML<LabelRecord> {

  @XmlElement(name = "id")
  private Integer id;

  @XmlElement(name = "name")
  private String name;

  @XmlElement(name = "contactinfo")
  private String contactInfo;

  @XmlElement(name = "profile")
  private String profile;

  @XmlElement(name = "data_quality")
  private String dataQuality;

  @Override
  public LabelRecord buildRecord() {
    return new LabelRecord()
        .setId(id)
        .setName(name)
        .setContactInfo(contactInfo)
        .setProfile(profile)
        .setDataQuality(dataQuality)
        .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
        .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
  }
}
