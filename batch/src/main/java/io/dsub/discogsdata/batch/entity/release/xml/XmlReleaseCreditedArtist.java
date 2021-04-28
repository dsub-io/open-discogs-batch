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
public class XmlReleaseCreditedArtist {

    @XmlAttribute(name = "id")
    private Long releaseId;


    @XmlElementWrapper(name = "extraartists")
    @XmlElement(name = "artist")
    private List<CreditedArtist> creditedArtists;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CreditedArtist {
        @XmlElement(name = "id")
        private Long id;
        @XmlElement(name = "name")
        private String name;
        @XmlElement(name = "role")
        private String role;

        private Long releaseId;
    }

    public List<CreditedArtist> getCreditedArtists() {
        return creditedArtists.parallelStream()
                .peek(item -> item.setReleaseId(releaseId))
                .collect(Collectors.toList());
    }
}
