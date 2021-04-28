package io.dsub.discogsdata.batch.entity.label.dto;

import io.dsub.discogsdata.batch.dto.RelationalDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LabelUrlDTO extends RelationalDTO {
    private String url;
    private long labelId;

    @Override
    public String getTblName() {
        return "label_url";
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
