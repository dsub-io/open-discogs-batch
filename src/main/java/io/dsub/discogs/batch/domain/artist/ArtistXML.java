package io.dsub.discogs.batch.domain.artist;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.opendiscogs.jooq.tables.records.ArtistRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;

@Data
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class ArtistXML implements BaseXML<ArtistRecord> {

  @XmlElement(name = "id")
  private Integer id;

  @XmlElement(name = "name")
  private String name;

  @XmlElement(name = "realname")
  private String realName;

  @XmlElement(name = "profile")
  private String profile;

  @XmlElement(name = "data_quality")
  private String dataQuality;

  @Override
  public ArtistRecord buildRecord() {
    return new ArtistRecord()
        .setId(id)
        .setName(name)
        .setRealName(realName)
        .setProfile(profile)
        .setDataQuality(dataQuality)
        .setLastModifiedAt(LocalDateTime.now(Clock.systemUTC()))
        .setCreatedAt(LocalDateTime.now(Clock.systemUTC()));
  }
}
