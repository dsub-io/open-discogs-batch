package io.dsub.discogsdata.batch.entity.artist.dto;

import io.dsub.discogsdata.batch.dto.RelationalDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistGroupDTO extends RelationalDTO {

    private long artistId;
    private long groupId;

    @Override
    public String getTblName() {
        return "artist_group";
    }

    @Override
    public boolean isRelational() {
        return true;
    }

    @Override
    public boolean isTimeBaseEntity() {
        return false;
    }
}
