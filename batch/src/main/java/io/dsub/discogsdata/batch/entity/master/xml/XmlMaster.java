package io.dsub.discogsdata.batch.entity.master.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMaster {
    @XmlAttribute(name = "id")
    private Long id;

    @XmlElement(name = "year")
    private Short year;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "data_quality")
    private String dataQuality;

    @XmlElement(name = "main_release")
    private Long mainRelease;
}
