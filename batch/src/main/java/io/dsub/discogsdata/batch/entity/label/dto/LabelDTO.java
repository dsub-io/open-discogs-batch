package io.dsub.discogsdata.batch.entity.label.dto;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LabelDTO extends BaseDTO {

    private long id;
    private String name;
    private String contactInfo;
    private String profile;
    private String dataQuality;

    @Override
    public String getTblName() {
        return "label";
    }

    @Override
    public boolean isRelational() {
        return false;
    }

    @Override
    public boolean isTimeBaseEntity() {
        return true;
    }
}
