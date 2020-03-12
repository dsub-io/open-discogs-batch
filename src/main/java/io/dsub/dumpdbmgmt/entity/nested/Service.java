package io.dsub.dumpdbmgmt.entity.nested;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.mapping.Field;

@With
@Getter
@AllArgsConstructor
// represents service that label have provided to.
// i.e. mastered, mixed, registered, distributed ...
public class Service {

    // represents release that the label have serviced.
    @Field(name = "release_id")
    private final Long releaseId;

    // represents service provided to given release.
    @Field(name = "service_note")
    private final String serviceNote;
}
