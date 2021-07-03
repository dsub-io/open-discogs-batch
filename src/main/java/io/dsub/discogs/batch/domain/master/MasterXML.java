package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.discogs.common.jooq.postgres.tables.records.MasterRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Data
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class MasterXML implements BaseXML<MasterRecord> {
    @XmlAttribute(name = "id")
    private Integer id;

    @XmlElement(name = "year")
    private Short year;

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "main_release")
    private Integer mainReleaseId;

    @XmlElement(name = "data_quality")
    private String dataQuality;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private List<String> genres;

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private List<String> styles;

    @Override
    public MasterRecord buildRecord() {
        return new MasterRecord()
                .setId(id)
                .setTitle(title)
                .setYear(year)
                .setDataQuality(dataQuality)
                .setCreatedAt(LocalDateTime.now(Clock.systemUTC()))
                .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
}
