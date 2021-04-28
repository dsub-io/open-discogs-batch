package io.dsub.discogsdata.batch.entity.artist.xml;

import io.dsub.discogsdata.batch.entity.artist.dto.ArtistAliasDTO;
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
public class XmlArtistAlias implements XmlRelation<ArtistAliasDTO> {
    @XmlElement(name = "id")
    private Long id;

    @XmlElementWrapper(name = "aliases")
    @XmlElement(name = "name")
    private List<Alias> aliases;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Alias {
        @XmlAttribute(name = "id")
        private Long id;
    }

    @Override
    public Collection<ArtistAliasDTO> toEntities() {
        return null;
    }
}
