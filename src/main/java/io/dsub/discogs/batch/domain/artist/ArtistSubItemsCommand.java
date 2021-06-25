package io.dsub.discogs.batch.domain.artist;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "artist")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class ArtistSubItemsCommand {

    @XmlElement(name = "id")
    private Long id;

    @XmlElementWrapper(name = "aliases")
    @XmlElement(name = "name")
    private List<Alias> aliases;

    @XmlElementWrapper(name = "groups")
    @XmlElement(name = "name")
    private List<Group> groups;

    @XmlElementWrapper(name = "members")
    @XmlElement(name = "name")
    private List<Member> members;

    @XmlElementWrapper(name = "namevariations")
    @XmlElement(name = "name")
    private List<String> nameVariations;

    @XmlElementWrapper(name = "urls")
    @XmlElement(name = "url")
    private List<String> urls;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Alias {
        @XmlAttribute(name = "id")
        private Long id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Group {

        @XmlAttribute(name = "id")
        private Long id;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Member {

        @XmlAttribute(name = "id")
        private Long id;
    }
}
