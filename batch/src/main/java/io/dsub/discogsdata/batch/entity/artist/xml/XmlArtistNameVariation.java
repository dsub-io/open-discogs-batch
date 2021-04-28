package io.dsub.discogsdata.batch.entity.artist.xml;

import io.dsub.discogsdata.batch.entity.artist.dto.ArtistNameVariationDTO;
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
public class XmlArtistNameVariation implements XmlRelation<ArtistNameVariationDTO> {
    @XmlElement(name = "id")
    private Long id;

    @XmlElementWrapper(name = "namevariations")
    @XmlElement(name = "name")
    private List<String> nameVariations;

    @Override
    public Collection<ArtistNameVariationDTO> toEntities() {
        return null;
    }
}
