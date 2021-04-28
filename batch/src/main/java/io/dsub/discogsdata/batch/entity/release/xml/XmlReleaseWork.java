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
public class XmlReleaseWork {

    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "companies")
    @XmlElement(name = "company")
    private List<Work> works = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Work {
        @XmlElement(name = "id")
        private Long id;
        @XmlElement(name = "name")
        private String name;
        @XmlElement(name = "entity_type_name")
        private String job;

        private Long releaseId;
    }

    public List<Work> getWorks() {
        return this.works.parallelStream()
                .peek(item -> item.setReleaseId(this.releaseId))
                .collect(Collectors.toList());
    }
}
