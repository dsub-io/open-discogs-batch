package io.dsub.dumpdbmgmt.entity.nested;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.mapping.Field;

@With
@Getter
@AllArgsConstructor
// service made from company (label)
// since we have no clue for xml dump which label type code represents,
// we will consider label as a company itself, which could present
// mixing, mastering, dist, etc...
//
// Some labels, therefore, will not have any released info, but only
// contain number of service notes, representing it is a company,
// not to be considered solely as label.
//
// This verbosity will not be fixed in any time soon, other than
// figuring out what label type represents in xml dump.
public class Company {
    // company(label) that has provided the service above.
    @Field(name = "label_id")
    private final Long labelId;

    // service note that the release have received.
    @Field(name = "service_note")
    private final String serviceNote;

}
