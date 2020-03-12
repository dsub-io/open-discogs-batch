package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.entity.nested.CreditedRelease;
import io.dsub.dumpdbmgmt.util.ArraysUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.index.Indexed;
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
    @Field(name = "members")
    private Long[] members = new Long[0];
    @Field(name = "groups")
    private Long[] groups = new Long[0];
    @Field(name = "aliases")
    private Long[] aliases = new Long[0];
    @Field(name = "releases")
    private Long[] releases = new Long[0];
    @Field(name = "master_releases")
    private Long[] masterReleases = new Long[0];
    @Field(name = "urls")
    private Set<String> urls = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "name_variations")
    private Set<String> nameVariations = Collections.synchronizedSet(new HashSet<>());

    //List of credits listed on each release.
    //Opposite (release) will also have credits with artist id as a nested document.
    @Field(name = "credits")
    private Set<CreditedRelease> credits = Collections.synchronizedSet(new HashSet<>());

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

    public Artist withAddCredits(CreditedRelease... creditedReleases) {
        Set<CreditedRelease> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.credits);
        idSet.addAll(Arrays.asList(creditedReleases));
        return this.withCredits(idSet);
    }

    public Artist withRemoveCredit(CreditedRelease creditedRelease) {
        Set<CreditedRelease> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.credits);
        idSet.removeIf(entry -> entry.equals(creditedRelease));
        return this.withCredits(idSet);
    }

    public Artist withAddMasterReleases(Long... masterReleaseIds) {
        Long[] arr = ArraysUtil.merge(masterReleases, masterReleaseIds);
        return this.withMasterReleases(arr);
    }

    public Artist withRemoveMasterRelease(Long masterReleaseId) {
        Long[] arr = ArraysUtil.remove(masterReleases, masterReleaseId);
        return this.withMasterReleases(arr);
    }

    public Artist withAddReleases(Long... releaseIds) {
        Long[] arr = ArraysUtil.merge(this.releases, releaseIds);
        return this.withReleases(arr);
    }

    public Artist withRemoveReleases(Long releaseId) {
        Long[] arr = ArraysUtil.remove(this.releases, releaseId);
        return this.withReleases(arr);
    }

    public Artist withAddAliasArtists(Long... artistIds) {
        Long[] arr = ArraysUtil.merge(this.aliases, artistIds);
        return this.withAliases(arr);
    }

    public Artist withRemoveAliasArtist(Long artistId) {
        Long[] arr = ArraysUtil.remove(this.aliases, artistId);
        return this.withAliases(arr);
    }

    public Artist withAddMemberArtists(Long... artistIds) {
        Long[] arr = ArraysUtil.merge(this.members, artistIds);
        return this.withMembers(arr);
    }

    public Artist withRemoveMemberArtist(Long artistId) {
        Long[] arr = ArraysUtil.remove(this.members, artistId);
        return this.withMembers(arr);
    }

    public Artist withAddGroupArtists(Long... artistIds) {
        Long[] arr = ArraysUtil.merge(this.groups, artistIds);
        return this.withGroups(arr);
    }

    public Artist withRemoveGroupArtist(Long artistId) {
        Long[] arr = ArraysUtil.remove(this.groups, artistId);
        return this.withGroups(arr);
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
