package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.service.ArtistService;
import io.dsub.dumpdbmgmt.xmlobj.XmlArtist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@StepScope
@Component("artistUpdateProcessor")
public class ArtistUpdateProcessor implements ItemProcessor<XmlArtist, Artist> {

    ArtistService artistService;

    public ArtistUpdateProcessor(ArtistService artistService) {
        this.artistService = artistService;
    }

    /**
     * Establishes relationship between aliases, groups and member artists.
     * As of discogs xml dump already contains corresponding relational info,
     * simply process them per each entity is the way to avoid duplicated
     * process.
     * <p>
     * i.e. a member will have a group, and the group will already have that
     * member in its own xml element section.
     */

    @Override
    public Artist process(XmlArtist item) {

        if (item.getGroups() == null && item.getAliases() == null && item.getMembers() == null) {
            // Nothing to process; Filter.
            return null;
        }

        Artist artist = artistService.findById(item.getId());

        if (artist != null && artist.getId() != null) {
            if (item.getAliases() != null) {
                for (XmlArtist.Aliase alias : item.getAliases()) {
                    Artist aliasArtist = artistService.findById(alias.getId());
                    if (aliasArtist != null && aliasArtist.getId() != null) {
                        artist = artist.withAddAliasArtists(aliasArtist);
                    }
                }
            }
            if (item.getMembers() != null) {
                for (XmlArtist.Member member : item.getMembers()) {
                    Artist memberArtist = artistService.findById(member.getId());
                    if (memberArtist != null && memberArtist.getId() != null) {
                        artist = artist.withAddMemberArtists(memberArtist);
                    }
                }
            }
            if (item.getGroups() != null) {
                for (XmlArtist.Group group : item.getGroups()) {
                    Artist groupArtist = artistService.findById(group.getId());
                    if (groupArtist != null && groupArtist.getId() != null) {
                        artist = artist.withAddGroupArtists(groupArtist);
                    }
                }
            }
            return artist;
        }
        // Not found artist as of filtered during ArtistProcessor.
        // Skip the entry.
        return null;
    }
}
