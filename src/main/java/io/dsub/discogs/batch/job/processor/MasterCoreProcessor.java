package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.master.MasterCommand;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.master.entity.Master;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class MasterCoreProcessor implements ItemProcessor<MasterCommand, BaseEntity> {
    @Override
    public BaseEntity process(MasterCommand command) throws Exception {
        if (command.getId() == null || command.getId() < 1) {
            return null;
        }
        ReflectionUtil.normalizeStringFields(command);
        return Master.builder()
                .id(command.getId())
                .dataQuality(command.getDataQuality())
                .title(command.getTitle())
                .year(command.getYear())
                .build();
    }
}