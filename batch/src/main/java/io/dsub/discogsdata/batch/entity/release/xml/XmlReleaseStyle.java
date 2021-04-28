package io.dsub.discogsdata.batch.entity.release.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseStyle {
    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private Set<String> styles;
}
