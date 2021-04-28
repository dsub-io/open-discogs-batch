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
public class XmlMasterStyle {
    @XmlAttribute(name = "id")
    private Long id;
    @XmlElementWrapper(name = "styles")
    @XmlElement(name = "style")
    private List<String> styles = new ArrayList<>();
}
