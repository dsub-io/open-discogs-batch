package io.dsub.discogsdata.batch.entity.artist.xml;

import io.dsub.discogsdata.batch.entity.artist.dto.ArtistUrlDTO;
import io.dsub.discogsdata.batch.xml.XmlRelation;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlArtistUrl implements XmlRelation<ArtistUrlDTO> {
    @XmlElement(name = "id")
    private Long id;

    @XmlElementWrapper(name = "urls")
    @XmlElement(name = "url")
    private List<String> urls = new LinkedList<>();

    @Override
    public Collection<ArtistUrlDTO> toEntities() {
        return null;
    }
}