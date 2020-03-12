package io.dsub.dumpdbmgmt.entity.nested;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.springframework.data.mongodb.core.mapping.Field;

@With
@Getter
@AllArgsConstructor
// Many releases contain credited artists information,
// which is labeled as "role".
// We will translate it as a notion of being "credited as".
public class CreditedArtist {
    // artist who got credited
    @Field(name = "artist_id")
    Long artistId;

    // credit record on given release.
    // could be a role, or just a credit.
    @Field(name = "credit")
    String credit;
}
