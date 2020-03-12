package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Label;
import io.dsub.dumpdbmgmt.entity.nested.Catalog;
import io.dsub.dumpdbmgmt.entity.nested.Service;
import io.dsub.dumpdbmgmt.service.LabelService;
import io.dsub.dumpdbmgmt.xmlobj.XmlRelease;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
@StepScope
public class LabelReferenceProcessor implements ItemProcessor<XmlRelease, Set<Label>> {

    private final LabelService labelService;

    public LabelReferenceProcessor(LabelService labelService) {
        this.labelService = labelService;
    }

    @Override
    public Set<Label> process(XmlRelease item) {
        Set<Label> labelSet = Collections.synchronizedSet(new HashSet<>());
        item.getLabels().forEach(entry -> {
            Label label = findLabel(entry.getId(), labelSet);
            if (label != null) {
                label = label.withAddCatalogs(new Catalog(item.getReleaseId(), entry.getCatno()));
                labelSet.add(label);
            }
        });
        item.getCompanies().forEach(entry -> {
            Label label = findLabel(entry.getId(), labelSet);
            if (label != null) {
                Service service = new Service(item.getReleaseId(), entry.getJob());
                label = label.withAddServices(service);
                labelSet.add(label);
            }
        });
        return labelSet;
    }

    private Label findLabel(Long id, Set<Label> labelSet) {
        for (Label label : labelSet) {
            if (label.getId().equals(id)) {
                labelSet.remove(label);
                return label;
            }
        }
        return labelService.findById(id);
    }
}
