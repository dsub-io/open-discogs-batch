package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.MasterRelease;
import io.dsub.dumpdbmgmt.service.MasterReleaseService;
import io.dsub.dumpdbmgmt.xmlobj.XmlRelease;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class MasterReleaseReferenceProcessor implements ItemProcessor<XmlRelease, MasterRelease> {

    private final MasterReleaseService masterReleaseService;

    public MasterReleaseReferenceProcessor(MasterReleaseService masterReleaseService) {
        this.masterReleaseService = masterReleaseService;
    }

    @Override
    public MasterRelease process(XmlRelease item) {
        if (item.getMaster() == null) {
            return null;
        }

        Long id = item.getMaster().getMasterId();

        if (id == null) {
            return null;
        }

        MasterRelease masterRelease = masterReleaseService.findById(id);

        if (masterRelease != null) {
            masterRelease = masterRelease.withAddReleases(item.getReleaseId());
            return masterRelease;
        }

        return null;
    }
}
