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
public class XmlMasterGenre {
    @XmlAttribute(name = "id")
    private Long id;
    @XmlElementWrapper(name = "genres")
    @XmlElement(name = "genre")
    private List<String> genres = new ArrayList<>();
}
