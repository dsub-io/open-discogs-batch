package io.dsub.dumpdbmgmt.entity.nested;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.mapping.Field;

@With
@Getter
@AllArgsConstructor
// credited releases that this artist got listed, and their
// information which can either be role or simple credit.
public class CreditedRelease {
    // listed release
    @Field(name = "release_id")
    Long releaseId;
    // content of credit
    @Field(name = "credit")
    String credit;
}
