package io.dsub.discogsdata.batch.entity.artist.xml;

import io.dsub.discogsdata.batch.entity.artist.dto.ArtistMemberDTO;
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
public class XmlArtistMember implements XmlRelation<ArtistMemberDTO> {

    @XmlElement(name = "id")
    private Long id;

    @XmlElementWrapper(name = "members")
    @XmlElement(name = "name")
    private List<Member> members;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Member {
        @XmlAttribute(name = "id")
        private Long id;
    }

    @Override
    public Collection<ArtistMemberDTO> toEntities() {
        return null;
    }
}
