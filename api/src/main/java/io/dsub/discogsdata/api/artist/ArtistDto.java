package io.dsub.discogsdata.api.artist;

import io.dsub.discogsdata.common.entity.artist.Artist;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ArtistDto {

    private final long id;
    private String resourceUrl;
    private String name;
    private String realName;
    private String dataQuality;
    private String profile;
    @Builder.Default
    private List<String> urls = new ArrayList<>();
    @Builder.Default
    private List<String> nameVariations = new ArrayList<>();
    @Builder.Default
    private List<Long> members = new ArrayList<>();
    @Builder.Default
    private List<Long> groups = new ArrayList<>();
    @Builder.Default
    private List<Long> aliases = new ArrayList<>();

    public static ArtistDto fromArtist(Artist artist, List<Long> members, List<Long> groups, List<Long> aliases, List<String> nameVariations, List<String> urls) {
        return ArtistDto.builder()
                .id(artist.getId())
                .dataQuality(artist.getDataQuality())
                .name(artist.getName())
                .realName(artist.getRealName())
                .profile(artist.getProfile())
                .resourceUrl("artists/" + artist.getId())
                .nameVariations(nameVariations)
                .urls(urls)
                .members(members)
                .groups(groups)
                .aliases(aliases)
                .build();
    }
}
