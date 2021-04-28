package io.dsub.discogsdata.batch.entity.release.xml;

import lombok.*;

import javax.xml.bind.annotation.*;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
public class XmlReleaseItem {

    @XmlAttribute(name = "id")
    private Long releaseId;
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
    @XmlElement(name = "master_id")
    private Master master;
    @XmlElement(name = "released")
    private String releaseDate;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Master {
        @XmlValue
        private Long masterId;
        @XmlAttribute(name = "is_main_release")
        private boolean isMaster;
    }

    public Long getMasterId() {
        return this.master == null ? null : this.master.getMasterId();
    }

    public boolean isMaster() {
        return this.master != null && this.master.isMaster;
    }
}
