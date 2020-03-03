package io.dsub.dumpdbmgmt.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.util.Arrays;
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
    @Field(name = "data_quality")
    private final String dataQuality;
    @Field(name = "release_year")
    @Indexed
    private final Short releaseYear;
    @DBRef(lazy = true)
    private Set<Release> releases = Collections.synchronizedSet(new HashSet<>());
    @DBRef(lazy = true)
    private Set<Artist> artists = Collections.synchronizedSet(new HashSet<>());
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

    public MasterRelease withAddArtists(Artist... artists) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.artists);
        modifiedSet.addAll(Arrays.asList(artists));
        return this.withArtists(modifiedSet);
    }

    public MasterRelease withRemoveArtist(Artist artist) {
        Set<Artist> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.artists);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(artist.getId()));
        return this.withArtists(modifiedSet);
    }

    public MasterRelease withAddReleases(Release... releases) {
        Set<Release> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.releases);
        modifiedSet.addAll(Arrays.asList(releases));
        return this.withReleases(modifiedSet);
    }

    public MasterRelease withRemoveRelease(Release release) {
        Set<Release> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.releases);
        modifiedSet.removeIf(candidate -> candidate.getId().equals(release.getId()));
        return this.withReleases(modifiedSet);
    }

}
