package io.dsub.discogs.batch.domain.master;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class MasterSubItemsCommand {

    @XmlAttribute(name = "id")
    private Long id;

    @XmlElementWrapper(name = "artists")
    @XmlElement(name = "artist")
    private List<Artist> artists;

    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private List<String> genres;

    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private List<String> styles;

    @XmlElementWrapper(name = "videos")
    @XmlElement(name = "video")
    private List<Video> videos;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Artist {

        @XmlElement(name = "id")
        private Long id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Video {
        @XmlElement(name = "title")
        private String title;

        @XmlElement(name = "description")
        private String description;

        @XmlAttribute(name = "src")
        private String url;
    }
}
