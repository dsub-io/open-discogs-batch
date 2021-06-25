package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.label.LabelSubItemsCommand;
import io.dsub.discogs.batch.domain.label.LabelSubItemsCommand.SubLabel;
import io.dsub.discogs.batch.job.registry.EntityIdRegistry;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.label.entity.Label;
import io.dsub.discogs.common.label.entity.LabelSubLabel;
import io.dsub.discogs.common.label.entity.LabelUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.dsub.discogs.batch.job.registry.EntityIdRegistry.Type.LABEL;

@RequiredArgsConstructor
public class LabelSubItemsProcessor implements ItemProcessor<LabelSubItemsCommand, Collection<BaseEntity>> {

    private final EntityIdRegistry idRegistry;

    @Override
    public Collection<BaseEntity> process(LabelSubItemsCommand command) {
        if (command.getId() == null || command.getId() < 1) {
            return null;
        }
        ReflectionUtil.normalizeStringFields(command);

        List<BaseEntity> items = new LinkedList<>();
        Long coreLabelId = command.getId();

        if (command.getUrls() != null) {

            command.getUrls().stream()
                    .filter(Objects::nonNull)
                    .filter(url -> !url.isBlank())
                    .distinct()
                    .map(url -> getLabelUrl(coreLabelId, url))
                    .forEach(items::add);
        }

        if (command.getSubLabels() != null) {
            command.getSubLabels().stream()
                    .filter(Objects::nonNull)
                    .map(SubLabel::getId)
                    .filter(this::isExistingLabel)
                    .distinct()
                    .map(subLabelId -> getSubLabel(coreLabelId, subLabelId))
                    .forEach(items::add);
        }

        return items.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isExistingLabel(Long labelId) {
        return idRegistry.exists(LABEL, labelId);
    }

    private LabelUrl getLabelUrl(Long coreLabelId, String url) {
        return LabelUrl.builder()
                .label(getLabel(coreLabelId))
                .url(url)
                .build();
    }

    private LabelSubLabel getSubLabel(Long coreLabelId, Long subLabelId) {
        return LabelSubLabel.builder()
                .parent(getLabel(coreLabelId))
                .subLabel(getLabel(subLabelId))
                .build();
    }

    private Label getLabel(Long coreLabelId) {
        return Label.builder().id(coreLabelId).build();
    }

}
