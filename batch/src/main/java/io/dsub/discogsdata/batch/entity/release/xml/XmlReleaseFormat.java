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
public class XmlReleaseFormat {

    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "formats")
    @XmlElement(name = "format")
    private List<Format> formats = new ArrayList<>();


    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Format {
        @XmlAttribute(name = "name")
        private String name;
        @XmlAttribute(name = "qty")
        private Integer qty;
        @XmlAttribute(name = "text")
        private String text;
        @XmlElementWrapper(name = "descriptions")
        @XmlElement(name = "description")
        private List<String> description;

        private Long releaseId;
    }

    public List<Format> getFormats() {
        return formats.parallelStream()
                .peek(format -> format.setReleaseId(releaseId))
                .collect(Collectors.toList());
    }
}
