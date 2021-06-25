package io.dsub.discogs.batch.domain.master;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class MasterCommand {
    @XmlAttribute(name = "id")
    private Long id;

    @XmlElement(name = "year")
    private Short year;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "data_quality")
    private String dataQuality;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private List<String> genres;

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private List<String> styles;
}
