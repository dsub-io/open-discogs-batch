package io.dsub.discogsdata.batch.entity.release.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseGenre {
    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private List<String> genres;
}
