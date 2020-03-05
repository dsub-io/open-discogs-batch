package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Label;
import io.dsub.dumpdbmgmt.service.LabelService;
import io.dsub.dumpdbmgmt.xmlobj.XmlLabel;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@StepScope
@Component("labelUpdateProcessor")
public class LabelUpdateProcessor implements ItemProcessor<XmlLabel, Label> {

    private LabelService labelService;

    public LabelUpdateProcessor(LabelService labelService) {
        this.labelService = labelService;
    }


    /**
     * Assign parent-child label relationship.
     *
     * @param item Objectified Xml document object.
     * @return transformed entity of Label.
     */

    @Override
    public Label process(XmlLabel item) {

        Label label = labelService.findById(item.getId());
        if (label == null) {
            return null;
        }
        if (item.getSubLabels() != null) {
            for (XmlLabel.SubLabel source : item.getSubLabels()) {
                Label subLabel = labelService.findById(source.getId());
                if (subLabel != null) {
                    subLabel = subLabel.withAddParentLabel(label);
                    labelService.save(subLabel);
                    label = label.withAddSubLabel(subLabel);
                }
            }
        }
        return label;
    }
}
