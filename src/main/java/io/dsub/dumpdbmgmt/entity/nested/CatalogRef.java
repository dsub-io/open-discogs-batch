package io.dsub.dumpdbmgmt.entity.nested;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.mapping.Field;

@With
@Getter
@AllArgsConstructor
// holding label id and the releases' catalog infos.
public class CatalogRef {
    // owner of this release as catalog
    @Field(name = "label_id")
    private final Long labelId;

    // release catalogued as following info
    @Field(name = "cat_no")
    private final String catNo;
}
