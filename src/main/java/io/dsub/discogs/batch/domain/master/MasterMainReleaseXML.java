package io.dsub.discogs.batch.domain.master;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.discogs.common.jooq.postgres.tables.records.MasterRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.time.Clock;
import java.time.LocalDateTime;

@Data
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class MasterMainReleaseXML implements BaseXML<MasterRecord> {

    @XmlAttribute(name = "id")
    private Integer id;

    @XmlElement(name = "main_release")
    private Integer mainReleaseId;

    @Override
    public MasterRecord buildRecord() {
        return new MasterRecord()
                .setId(id)
                .setMainReleaseId(mainReleaseId)
                .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()));
    }
}
