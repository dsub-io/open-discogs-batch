package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.BaseXML;
import java.time.LocalDateTime;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class MasterMainReleaseXML implements BaseXML<MasterMainReleaseAssignment> {

  @XmlAttribute(name = "id")
  private Integer releaseId;

  @XmlElement(name = "master_id")
  private Master master;

  @Override
  public MasterMainReleaseAssignment buildRecord(LocalDateTime observedAt) {
    Integer targetMasterId =
        master != null && master.isMainRelease() ? master.getMasterId() : null;
    return new MasterMainReleaseAssignment(
        releaseId, targetMasterId, observedAt);
  }

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Master {

    @XmlValue
    private Integer masterId;

    @XmlAttribute(name = "is_main_release")
    private boolean mainRelease;
  }
}
