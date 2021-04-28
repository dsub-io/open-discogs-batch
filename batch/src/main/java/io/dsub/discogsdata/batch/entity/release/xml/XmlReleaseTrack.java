package io.dsub.discogsdata.batch.entity.release.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseTrack {

    @XmlElementWrapper(name = "tracklist")
    @XmlElement(name = "track")
    private Set<Track> tracks = new HashSet<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Track {
        @XmlElement(name = "position")
        private String position;
        @XmlElement(name = "title")
        private String title;
        @XmlElement(name = "duration")
        private String duration;
    }

}
