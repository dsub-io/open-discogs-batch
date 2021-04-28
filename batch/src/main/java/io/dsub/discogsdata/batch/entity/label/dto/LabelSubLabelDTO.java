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
public class LabelSubLabelDTO extends RelationalDTO {

    private long parentLabelId;
    private long subLabelId;

    @Override
    public String getTblName() {
        return "label_sub_label";
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
