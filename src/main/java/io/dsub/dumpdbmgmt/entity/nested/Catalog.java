package io.dsub.dumpdbmgmt.entity.nested;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.mapping.Field;

@With
@Getter
@AllArgsConstructor
// Catalog of releases for labels
public class Catalog {
    // release id ref in DB
    @Field(name = "release_id")
    private final Long releaseId;

    // category number for releases
    @Field(name = "cat_no")
    private final String catNo;
}
