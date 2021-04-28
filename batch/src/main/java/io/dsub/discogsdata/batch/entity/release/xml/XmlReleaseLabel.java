package io.dsub.discogsdata.batch.entity.release.xml;


import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseLabel {

    @XmlElementWrapper(name = "labels")
    @XmlElement(name = "label")
    private Set<Label> labels = new HashSet<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Label {
        @XmlAttribute(name = "catno")
        private String categoryNumber;
        @XmlAttribute(name = "id")
        private Long id;
        @XmlAttribute(name = "name")
        private String labelName;
    }
}
