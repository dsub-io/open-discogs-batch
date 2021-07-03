package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.jooq.postgres.tables.records.MasterRecord;
import io.dsub.discogs.common.master.entity.Master;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class MasterCoreProcessor implements ItemProcessor<MasterXML, MasterRecord> {
    @Override
    public MasterRecord process(MasterXML master) throws Exception {
        if (master.getId() == null || master.getId() < 1) {
            return null;
        }
        ReflectionUtil.normalizeStringFields(master);
        return master.buildRecord();
    }
}