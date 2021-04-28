package io.dsub.discogsdata.batch.entity.artist.xml;

import io.dsub.discogsdata.batch.entity.artist.dto.ArtistGroupDTO;
import io.dsub.discogsdata.batch.xml.XmlRelation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlArtistGroup implements XmlRelation<ArtistGroupDTO> {
    @XmlElement(name = "id")
    private Long id;

    @XmlElementWrapper(name = "groups")
    @XmlElement(name = "name")
    private List<Group> groups;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Group {
        @XmlAttribute(name = "id")
        private Long id;
    }

    @Override
    public Collection<ArtistGroupDTO> toEntities() {
        return null;
    }
}