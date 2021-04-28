package io.dsub.discogsdata.batch.entity.label.xml;

import io.dsub.discogsdata.batch.entity.label.dto.LabelDTO;
import io.dsub.discogsdata.batch.xml.XmlEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlLabel implements XmlEntity<LabelDTO> {
    @XmlElement(name = "id")
    private Long id;
    @XmlElement(name = "name")
    private String name;
    @XmlElement(name = "contactinfo")
    private String contactInfo;
    @XmlElement(name = "profile")
    private String profile;
    @XmlElement(name = "data_quality")
    private String dataQuality;

    @Override
    public LabelDTO toEntity() {
        return null;
    }
}
