package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.label.LabelCommand;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.label.entity.Label;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class LabelCoreProcessor implements ItemProcessor<LabelCommand, BaseEntity> {
    @Override
    public BaseEntity process(LabelCommand command) throws Exception {
        if (command.getId() == null || command.getId() < 1) {
            return null;
        }
        ReflectionUtil.normalizeStringFields(command);
        return Label.builder()
                .id(command.getId())
                .name(command.getName())
                .profile(command.getProfile())
                .dataQuality(command.getProfile())
                .contactInfo(command.getContactInfo())
                .build();
    }
}