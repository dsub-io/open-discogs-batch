package io.dsub.discogsdata.batch.entity.release.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseIdentifier {

    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "identifiers")
    @XmlElement(name = "identifier")
    private List<Identifier> identifiers = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Identifier {
        @XmlAttribute(name = "description")
        private String description;
        @XmlAttribute(name = "type")
        private String type;
        @XmlAttribute(name = "value")
        private String value;

        private long releaseId;
    }

    public List<Identifier> getIdentifiers() {
        return this.identifiers.parallelStream()
                .peek(item -> item.setReleaseId(releaseId))
                .collect(Collectors.toList());
    }
}
