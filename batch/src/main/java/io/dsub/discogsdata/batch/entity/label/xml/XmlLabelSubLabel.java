package io.dsub.discogsdata.batch.entity.label.xml;

import io.dsub.discogsdata.batch.entity.label.dto.LabelSubLabelDTO;
import io.dsub.discogsdata.batch.xml.XmlRelation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlLabelSubLabel implements XmlRelation<LabelSubLabelDTO> {
    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "profile")
    private String profile;

    @XmlElementWrapper(name = "sublabels")
    @XmlElement(name = "label")
    private List<SubLabel> SubLabels;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SubLabel {
        @XmlValue
        private String name;
        @XmlAttribute(name = "id")
        private Long id;
    }

    @Override
    public Collection<LabelSubLabelDTO> toEntities() {
        return null;
    }
}
