package io.dsub.discogs.batch.domain.label;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
public class LabelCommand {
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
}
