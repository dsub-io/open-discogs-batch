package io.dsub.discogsdata.batch.entity.release.xml;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "release")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlReleaseVideo {

    @XmlAttribute(name = "id")
    private Long releaseId;

    @XmlElementWrapper(name = "videos")
    @XmlElement(name = "video")
    private List<Video> videos = new ArrayList<>();

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Video {
        @XmlElement(name = "title")
        private String title;
        @XmlElement(name = "description")
        private String description;
        @XmlAttribute(name = "src")
        private String url;

        private Long releaseId;
    }

    public List<Video> getVideos() {
        return videos.stream()
                .peek(video -> video.setReleaseId(this.releaseId))
                .collect(Collectors.toList());
    }
}
