package io.dsub.discogsdata.batch.entity.release.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseArtist {
    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "artists")
    @XmlElement(name = "artist")
    private List<AlbumArtist> releaseArtists;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AlbumArtist {
        @XmlElement(name = "id")
        private Long id;
        @XmlElement(name = "name")
        private String name;

        private Long releaseId;
    }

    public List<AlbumArtist> getReleaseArtists() {
        return releaseArtists.parallelStream()
                .peek(item -> item.setReleaseId(releaseId))
                .collect(Collectors.toList());
    }
}
