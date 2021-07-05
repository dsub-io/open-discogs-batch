package io.dsub.discogs.batch.domain.artist;

import io.dsub.discogs.batch.domain.BaseXML;
import io.dsub.discogs.common.jooq.tables.records.ArtistRecord;
import java.time.Clock;
import java.time.LocalDateTime;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
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
