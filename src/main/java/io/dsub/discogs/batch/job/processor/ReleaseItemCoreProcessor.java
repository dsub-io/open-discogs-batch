package io.dsub.discogs.batch.job.processor;

import io.dsub.discogs.batch.domain.release.ReleaseItemCommand;
import io.dsub.discogs.batch.util.DefaultMalformedDateParser;
import io.dsub.discogs.batch.util.MalformedDateParser;
import io.dsub.discogs.batch.util.ReflectionUtil;
import io.dsub.discogs.common.entity.BaseEntity;
import io.dsub.discogs.common.release.entity.ReleaseItem;
import org.springframework.batch.item.ItemProcessor;

public class ReleaseItemCoreProcessor implements ItemProcessor<ReleaseItemCommand, BaseEntity> {

    private final MalformedDateParser parser = new DefaultMalformedDateParser();

    @Override
    public BaseEntity process(ReleaseItemCommand command) throws Exception {

        if (command.getId() == null || command.getId() < 1) {
            return null;
        }
        ReflectionUtil.normalizeStringFields(command);
        return ReleaseItem.builder()
                .id(command.getId())
                .title(command.getTitle())
                .country(command.getCountry())
                .dataQuality(command.getDataQuality())
                .releaseDate(parser.parse(command.getReleaseDate()))
                .hasValidDay(parser.isDayValid(command.getReleaseDate()))
                .hasValidMonth(parser.isMonthValid(command.getReleaseDate()))
                .hasValidYear(parser.isYearValid(command.getReleaseDate()))
                .listedReleaseDate(command.getReleaseDate())
                .isMaster(command.getMaster() != null && command.getMaster().isMaster())
                .notes(command.getNotes())
                .status(command.getStatus())
                .build();
    }
}
