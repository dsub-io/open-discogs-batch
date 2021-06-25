package io.dsub.discogs.batch.domain.label;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "label")
@XmlAccessorType(XmlAccessType.FIELD)
@EqualsAndHashCode(callSuper = false)
public class LabelSubItemsCommand {

    @XmlElement(name = "id")
    private Long id;

    @XmlElementWrapper(name = "sublabels")
    @XmlElement(name = "label")
    private List<SubLabel> SubLabels;

    @XmlElementWrapper(name = "urls")
    @XmlElement(name = "url")
    private List<String> urls;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SubLabel {

        @XmlValue
        private String name;

        @XmlAttribute(name = "id")
        private Long id;
    }
}
