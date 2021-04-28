package io.dsub.discogsdata.batch.entity.artist.xml;

import io.dsub.discogsdata.batch.entity.artist.dto.ArtistDTO;
import io.dsub.discogsdata.batch.xml.XmlEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlArtist implements XmlEntity<ArtistDTO> {

    @XmlElement(name = "id")
    private Long id;
    @XmlElement(name = "name")
    private String name;
    @XmlElement(name = "realname")
    private String realName;
    @XmlElement(name = "profile")
    private String profile;
    @XmlElement(name = "data_quality")
    private String dataQuality;

    @Override
    public ArtistDTO toEntity() {
        return null;
    }
}