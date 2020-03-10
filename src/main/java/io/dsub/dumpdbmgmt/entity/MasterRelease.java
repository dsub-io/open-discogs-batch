package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.util.ArraysUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@With
@Getter
@AllArgsConstructor
@Document(collection = "master_releases")
public final class MasterRelease extends BaseEntity {
    @Id
    private final Long id;
    @Field(name = "title")
    @Indexed
    private final String title;
    @Field(name = "release_year")
    @Indexed
    private final Short releaseYear;
    @Field(name = "data_quality")
    private final String dataQuality;
    @Field(name = "releases")
    private Long[] releases = new Long[0];
    @Field(name = "artists")
    private Long[] artists = new Long[0];
    @Field(name = "genres")
    @Indexed
    private Set<String> genres = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "styles")
    @Indexed
    private Set<String> styles = Collections.synchronizedSet(new HashSet<>());

    protected MasterRelease() {
        this.id = null;
        this.title = null;
        this.releaseYear = null;
        this.dataQuality = null;
    }

    public MasterRelease(Long id) {
        this.id = id;
        this.title = null;
        this.releaseYear = null;
        this.dataQuality = null;
    }

    public MasterRelease withAddArtists(Long... artistIds) {
        Long[] arr = ArraysUtil.merge(this.artists, artistIds);
        return this.withArtists(arr);
    }

    public MasterRelease withRemoveArtist(Long artistId) {
        Long[] arr = ArraysUtil.remove(this.artists, artistId);
        return this.withArtists(arr);
    }

    public MasterRelease withAddReleases(Long... releaseIds) {
        Long[] arr = ArraysUtil.merge(this.releases, releaseIds);
        return this.withReleases(arr);
    }

    public MasterRelease withRemoveRelease(Long releaseId) {
        Long[] arr = ArraysUtil.remove(this.releases, releaseId);
        return this.withReleases(arr);
    }

}
