package io.dsub.discogsdata.batch.entity.master.xml;

import io.dsub.discogsdata.common.exception.UnsupportedOperationException;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@XmlRootElement(name = "master")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlMasterVideo {

    @XmlAttribute(name = "id")
    private Long id;

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

        private Long masterId;

        public Long getMasterId() {
            if (masterId == null) {
                throw new UnsupportedOperationException(
                        "XmlMasterRelation.Video.class must be pulled from XmlMasterRelation.class");
            }
            return masterId;
        }
    }

    public List<Video> getVideos() {
        this.videos.forEach(video -> video.setMasterId(id));
        return this.videos;
    }
}
