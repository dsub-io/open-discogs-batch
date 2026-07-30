package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.opendiscogs.jooq.tables.records.MasterRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class MasterMainReleaseXML implements BaseXML<MasterRecord> {

  @XmlAttribute(name = "id")
  private Integer id;

  @XmlElement(name = "main_release")
  private Integer mainReleaseId;

  @Override
  public MasterRecord buildRecord() {
    return new MasterRecord()
        .setId(id)
        .setMainReleaseId(mainReleaseId)
        .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
  }
}
