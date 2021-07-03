package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterMainReleaseXML;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.common.jooq.postgres.tables.records.MasterRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class MasterMainReleaseItemProcessor implements ItemProcessor<MasterMainReleaseXML, MasterRecord> {

    private final EntityIdRegistry idRegistry;

    @Override
    public MasterRecord process(MasterMainReleaseXML item) throws Exception {
        if (item == null || item.getId() == null || item.getMainReleaseId() == null) {
            return null;
        }

        if (!idRegistry.exists(EntityIdRegistry.Type.RELEASE, item.getMainReleaseId())) {
            return null;
        }

        return item.buildRecord();
    }
}
