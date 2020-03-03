package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.entity.intermed.ArtistCredit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.util.*;

@With
@Getter
@AllArgsConstructor
@Document(collection = "artists")
public final class Artist extends BaseEntity {

    @Id
    private final Long id;
    @Field(name = "name")
    @Indexed
    private final String name;
    @Field(name = "real_name")
    private final String realName;
    @Field(name = "profile")
    private final String profile;
    @Field(name = "data_quality")
    private final String dataQuality;
    @DBRef(lazy = true)
    private Set<Artist> members = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<Artist> groups = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<Artist> alias = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<Release> releases = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<MasterRelease> masterReleases = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<ArtistCredit> credits = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "urls")
    private Set<String> urls = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "name_variations")
    private Set<String> nameVariations = Collections.synchronizedSet(new HashSet<>());

    public Artist() {
        this.id = null;
        this.name = null;
        this.realName = null;
        this.profile = null;
        this.dataQuality = null;
    }

    public Artist(Long id) {
        this.id = id;
        this.name = null;
        this.realName = null;
        this.profile = null;
        this.dataQuality = null;
    }

    public Artist withAddReleases(Release... releases) {
        Set<Release> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.releases);
        modifiedSet.addAll(Arrays.asList(releases));
        return this.withReleases(modifiedSet);
    }

    public Artist withRemoveRelease(Release release) {
        Set<Release> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.releases);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(release.getId()));
        return this.withReleases(modifiedSet);
    }

    public Artist withAddMasterReleases(MasterRelease... masterReleases) {
        Set<MasterRelease> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.masterReleases);
        modifiedSet.addAll(Arrays.asList(masterReleases));
        return this.withMasterReleases(modifiedSet);
    }

    public Artist withRemoveMasterRelease(MasterRelease masterRelease) {
        Set<MasterRelease> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.masterReleases);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(masterRelease.getId()));
        return this.withMasterReleases(modifiedSet);
    }

    public Artist withAddCredits(ArtistCredit... artistCredits) {
        Set<ArtistCredit> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.credits);
        modifiedSet.addAll(Arrays.asList(artistCredits));
        return this.withCredits(modifiedSet);
    }

    public Artist withRemoveCredit(ArtistCredit artistCredit) {
        Set<ArtistCredit> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.credits);
        modifiedSet.removeIf(candidate -> candidate.getCredit().equals(artistCredit.getCredit()));
        return this.withCredits(modifiedSet);
    }


    public Artist withAddAliasArtists(Artist... artists) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.alias);
        modifiedSet.forEach(entry -> {
            for (Artist artist : artists) {
                if (!entry.getId().equals(artist.getId())) {
                    modifiedSet.add(artist);
                }
            }
        });
        return this.withAlias(modifiedSet);
    }

    public Artist withRemoveAliasArtist(Artist artist) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.alias);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(artist.getId()));
        return this.withAlias(modifiedSet);
    }

    public Artist withAddMemberArtists(Artist... artists) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.members);
        modifiedSet.forEach(entry -> {
            for (Artist artist : artists) {
                if (!entry.getId().equals(artist.getId())) {
                    modifiedSet.add(artist);
                }
            }
        });
        return this.withMembers(modifiedSet);
    }

    public Artist withRemoveMemberArtist(Artist artist) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.members);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(artist.getId()));
        return this.withMembers(modifiedSet);
    }

    public Artist withAddGroupArtists(Artist... artists) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.groups);
        modifiedSet.forEach(entry -> {
            for (Artist artist : artists) {
                if (!entry.getId().equals(artist.getId())) {
                    modifiedSet.add(artist);
                }
            }
        });
        return this.withGroups(modifiedSet);
    }

    public Artist withRemoveGroupArtist(Artist artist) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.groups);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(artist.getId()));
        return this.withGroups(modifiedSet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return Objects.equals(this.id, artist.id) &&
                Objects.equals(name, artist.name) &&
                Objects.equals(realName, artist.realName) &&
                Objects.equals(profile, artist.profile) &&
                Objects.equals(dataQuality, artist.dataQuality);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, realName, profile, dataQuality);
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", realName='" + realName + '\'' +
                ", profile='" + profile + '\'' +
                ", dataQuality='" + dataQuality + '\'' +
                '}';
    }
}
