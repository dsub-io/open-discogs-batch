package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Label;
import io.dsub.dumpdbmgmt.service.LabelService;
import io.dsub.dumpdbmgmt.xmlobj.XmlLabel;
import lombok.Synchronized;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@StepScope
@Component("labelProcessor")
public class LabelProcessor implements ItemProcessor<XmlLabel, Label> {

    LabelService labelService;

    public LabelProcessor(LabelService labelService) {
        this.labelService = labelService;
    }

    /**
     * Simple process to assign xml elements and attributes to a Label object.
     * If profile contains "[b]DO NOT USE.[/b]", indicates the object is
     * marked as disposed, hence filter as null.
     */

    @Override
    public Label process(XmlLabel item) {
        if (item.getId() == null) {
            return null;
        }

        Label label = new Label(item.getId());

        if (item.getProfile() != null) {
            if (item.getProfile().contains("[b]DO NOT USE.[/b]")) {
                return null;
            }
            label = label.withProfile(item.getProfile());
        }

        if (item.getName() != null) {
            label = label.withName(item.getName());
        }

        if (item.getContactInfo() != null) {
            label = label.withContactInfo(item.getContactInfo());
        }

        if (item.getDataQuality() != null) {
            label = label.withDataQuality(item.getDataQuality());
        }

        if (item.getUrls() != null) {
            label = label.withUrls(item.getUrls());
        }

        if (item.getSubLabels() != null) {
            for (XmlLabel.SubLabel source : item.getSubLabels()) {
                if (source.getId() != null) {
                    addParentLabel(source.getId(), label.getId());
                    label = label.withAddSubLabel(source.getId());
                }
            }
        }

        return label;
    }

    @Synchronized
    private void addParentLabel(Long childId, Long parentId) {
        Label subLabel = labelService.findById(childId);
        if (subLabel != null) {
            subLabel = subLabel.withAddParentLabel(parentId);
            labelService.save(subLabel);
        }
    }
}
