package io.dsub.dumpdbmgmt.batch.processor;

import io.dsub.dumpdbmgmt.entity.Artist;
import io.dsub.dumpdbmgmt.xmlobj.XmlArtist;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@StepScope
@Component("artistProcessor")
public class ArtistProcessor implements ItemProcessor<XmlArtist, Artist> {

    @Override
    public Artist process(XmlArtist item) {
        if (item.getId() == null) {
            return null;
        }

        Artist artist = new Artist(item.getId());

        if (item.getProfile() != null) {
            if (item.getProfile().contains("[b]DO NOT USE.[/b]")) {
                return null;
            } else {
                artist = artist.withProfile(item.getProfile());
            }
        }

        if (item.getNameVariations() != null) {
            artist = artist.withNameVariations(item.getNameVariations());
        }

        if (item.getName() != null) {
            artist = artist.withName(item.getName());
        }

        if (item.getUrls() != null) {
            artist = artist.withUrls(item.getUrls());
        }

        if (item.getDataQuality() != null) {
            artist = artist.withDataQuality(item.getDataQuality());
        }

        if (item.getAliases() != null) {
            for (XmlArtist.Aliase alias : item.getAliases()) {
                artist = artist.withAddAliasArtists(alias.getId());
            }
        }

        if (item.getMembers() != null) {
            for (XmlArtist.Member member : item.getMembers()) {
                artist = artist.withAddMemberArtists(member.getId());
            }
        }

        if (item.getGroups() != null) {
            for (XmlArtist.Group group : item.getGroups()) {
                artist = artist.withAddGroupArtists(group.getId());
            }
        }

        return artist;

    }
}
