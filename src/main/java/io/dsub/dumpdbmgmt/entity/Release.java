package io.dsub.dumpdbmgmt.entity;

import io.dsub.dumpdbmgmt.entity.nested.Format;
import io.dsub.dumpdbmgmt.entity.nested.Identifier;
import io.dsub.dumpdbmgmt.entity.nested.Track;
import io.dsub.dumpdbmgmt.entity.nested.Video;
import io.dsub.dumpdbmgmt.util.ArraysUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Id;
import java.time.LocalDate;
import java.util.*;

@With
@Getter
@AllArgsConstructor
@Document(collection = "releases")
public final class Release extends BaseEntity {
    @Id
    private final Long id;
    @Field(name = "status")
    private final String status;
    @Field(name = "title")
    @Indexed
    private final String title;
    @Field(name = "country")
    private final String country;
    @Field(name = "notes")
    private final String notes;
    @Field(name = "is_main")
    private final Boolean isMain;
    @Field(name = "data_quality")
    private final String dataQuality;
    @Field(name = "release_date")
    @Indexed
    private final LocalDate releaseDate;
    @Field(name = "view_date")
    @Indexed
    private final String viewDate;
    @Field(name = "master_release")
    private final Long masterRelease;
    @Field(name = "artists")
    private Long[] artists = new Long[0];
    @Field(name = "tracks")
    @Indexed
    private Set<Track> tracks = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "identifiers")
    private Set<Identifier> identifiers = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "formats")
    @Indexed
    private Set<Format> formats = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "videos")
    private Set<Video> videos = Collections.synchronizedSet(new HashSet<>());
    @Field("artist_credits")
    private Set<ObjectId> creditedArtists = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "label_releases")
    private Set<ObjectId> labelReleases = Collections.synchronizedSet(new HashSet<>());
    @Field(name = "company_releases")
    private Set<ObjectId> companyReleases = Collections.synchronizedSet(new HashSet<>());

    protected Release() {
        this.id = null;
        this.status = null;
        this.isMain = false;
        this.country = null;
        this.notes = null;
        this.title = null;
        this.masterRelease = null;
        this.dataQuality = null;
        this.releaseDate = null;
        this.viewDate = null;
    }

    public Release(Long id) {
        this.id = id;
        this.status = null;
        this.isMain = false;
        this.country = null;
        this.notes = null;
        this.title = null;
        this.masterRelease = null;
        this.dataQuality = null;
        this.releaseDate = null;
        this.viewDate = null;
    }

    @Override
    public String toString() {
        return "Release{" +
                "id=" + getId() +
                ", status='" + status + '\'' +
                ", title='" + title + '\'' +
                ", country='" + country + '\'' +
                ", notes='" + notes + '\'' +
                ", isMain=" + isMain +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Release release = (Release) o;
        return Objects.equals(getId(), release.getId()) &&
                Objects.equals(status, release.status) &&
                Objects.equals(title, release.title) &&
                Objects.equals(country, release.country) &&
                Objects.equals(notes, release.notes) &&
                Objects.equals(isMain, release.isMain);
    }

    public Release withAddArtists(Long... artistIds) {
        return this.withArtists(ArraysUtil.merge(this.artists, artistIds));
    }

    public Release withRemoveArtist(Long artistId) {
        return this.withArtists(ArraysUtil.remove(this.artists, artistId));
    }

    public Release withAddTracks(Track... tracks) {
        Set<Track> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.tracks);
        modifiedSet.addAll(Arrays.asList(tracks));
        return this.withTracks(modifiedSet);
    }

    public Release withRemoveTrack(Track track) {
        Set<Track> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.tracks);
        modifiedSet.removeIf(candidate -> candidate.getTitle().equals(track.getTitle()));
        return this.withTracks(modifiedSet);
    }

    public Release withAddIdentifiers(Identifier... identifiers) {
        Set<Identifier> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.identifiers);
        modifiedSet.addAll(Arrays.asList(identifiers));
        return this.withIdentifiers(modifiedSet);
    }

    public Release withRemoveIdentifier(Identifier identifier) {
        Set<Identifier> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.identifiers);
        modifiedSet.removeIf(candidate -> candidate.equals(identifier));
        return this.withIdentifiers(modifiedSet);
    }

    public Release withAddFormats(Format... formats) {
        Set<Format> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.formats);
        modifiedSet.addAll(Arrays.asList(formats));
        return this.withFormats(modifiedSet);
    }

    public Release withRemoveFormat(Format format) {
        Set<Format> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.formats);
        modifiedSet.removeIf(candidate -> candidate.getName().equals(format.getName()));
        return this.withFormats(modifiedSet);
    }

    public Release withAddVideos(Video... videos) {
        Set<Video> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.videos);
        modifiedSet.addAll(Arrays.asList(videos));
        return this.withVideos(modifiedSet);
    }

    public Release withRemoveVideo(Video video) {
        Set<Video> modifiedSet = Collections.synchronizedSet(new HashSet<>());
        modifiedSet.addAll(this.videos);
        modifiedSet.removeIf(candidate -> candidate.getUrl().equals(video.getUrl()));
        return this.withVideos(modifiedSet);
    }

    public Release withAddLabelReleases(ObjectId... labelReleaseIds) {
        Set<ObjectId> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.labelReleases);
        idSet.addAll(Arrays.asList(labelReleaseIds));
        return this.withLabelReleases(idSet);
    }

    public Release withRemoveLabelRelease(ObjectId labelReleaseId) {
        Set<ObjectId> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.labelReleases);
        idSet.removeIf(entry -> entry.equals(labelReleaseId));
        return this.withLabelReleases(idSet);
    }

    public Release withAddCompaniesReleases(ObjectId... workReleaseIds) {
        Set<ObjectId> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.companyReleases);
        idSet.addAll(Arrays.asList(workReleaseIds));
        return this.withCompanyReleases(idSet);
    }

    public Release withRemoveCompanyRelease(ObjectId workReleaseId) {
        Set<ObjectId> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.companyReleases);
        idSet.removeIf(entry -> entry.equals(workReleaseId));
        return this.withCompanyReleases(idSet);
    }

    public Release withAddCreditedArtists(ObjectId... creditedArtistsIds) {
        Set<ObjectId> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.creditedArtists);
        idSet.addAll(Arrays.asList(creditedArtistsIds));
        return this.withCreditedArtists(idSet);
    }

    public Release withRemoveCreditedArtists(ObjectId artistCreditId) {
        Set<ObjectId> idSet = Collections.synchronizedSet(new HashSet<>());
        idSet.addAll(this.creditedArtists);
        idSet.removeIf(entry -> entry.equals(artistCreditId));
        return this.withCreditedArtists(idSet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, title, country, notes, isMain);
    }

    // == Thread-Safe Collections ==
}
