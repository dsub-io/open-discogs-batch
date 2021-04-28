package io.dsub.discogsdata.batch.entity.artist.dto;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistDTO extends BaseDTO {

    private long id;
    private String name;
    private String realName;
    private String profile;
    private String dataQuality;

    @Override
    public boolean isRelational() {
        return false;
    }

    @Override
    public String getTblName() {
        return "artist";
    }

    @Override
    public boolean isTimeBaseEntity() {
        return true;
    }
}
