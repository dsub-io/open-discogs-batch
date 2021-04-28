package io.dsub.discogsdata.batch.entity.master.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMasterArtist {

    @XmlAttribute(name = "id")
    private Long id;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Artist {

        @XmlElement(name = "id")
        private Long id;
    }

    @XmlElementWrapper(name = "artists")
    @XmlElement(name = "artist")
    private List<Artist> artists = new ArrayList<>();
}
