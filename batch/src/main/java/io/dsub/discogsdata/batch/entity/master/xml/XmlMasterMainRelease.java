package io.dsub.discogsdata.batch.entity.master.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMasterMainRelease {
    @XmlAttribute(name = "id")
    private Long id;
    @XmlElement(name = "main_release")
    private Long mainRelease;
}
