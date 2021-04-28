package io.dsub.discogsdata.batch.entity.label.xml;

import io.dsub.discogsdata.batch.entity.label.dto.LabelUrlDTO;
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
public class XmlLabelUrl implements XmlRelation<LabelUrlDTO> {
    
    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "profile")
    private String profile;

    @XmlElementWrapper(name = "urls")
    @XmlElement(name = "url")
    private List<String> urls;

    @Override
    public Collection<LabelUrlDTO> toEntities() {
        return null;
    }
}
