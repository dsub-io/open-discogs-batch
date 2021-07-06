package io.dsub.discogs.batch.domain.label;

import io.dsub.discogs.batch.domain.SubItemXML;
import io.dsub.discogs.jooq.tables.records.LabelSubLabelRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class LabelSubItemsXML {

  @XmlElement(name = "id")
  private Integer id;

  @XmlElementWrapper(name = "sublabels")
  @XmlElement(name = "label")
  private List<LabelSubLabelXML> labelSubLabels = new ArrayList<>();

  @XmlElementWrapper(name = "urls")
  @XmlElement(name = "url")
  private List<String> urls = new ArrayList<>();

  @Data
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class LabelSubLabelXML implements SubItemXML<LabelSubLabelRecord> {

    @XmlValue
    private String name;

    @XmlAttribute(name = "id")
    private Integer subLabelId;

    @Override
    public LabelSubLabelRecord getRecord(int parentId) {
      return new LabelSubLabelRecord()
          .setParentLabelId(parentId)
          .setSubLabelId(subLabelId)
          .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
          .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
  }
}
