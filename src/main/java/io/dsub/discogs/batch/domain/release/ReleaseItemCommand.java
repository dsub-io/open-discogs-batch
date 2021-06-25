package io.dsub.discogs.batch.domain.release;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class ReleaseItemCommand {
    @XmlAttribute(name = "id")
    private Long id;

    @XmlAttribute(name = "status")
    private String status;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "country")
    private String country;

    @XmlElement(name = "notes")
    private String notes;

    @XmlElement(name = "data_quality")
    private String dataQuality;

    @XmlElement(name = "released")
    private String releaseDate;

    @XmlElement(name = "master_id")
    private Master master;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private List<String> genres;

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private List<String> styles;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Master {

        @XmlValue
        Long masterId;

        @XmlAttribute(name = "is_main_release")
        boolean isMaster;

        public Long getMasterId() {
            return masterId;
        }
    }
}