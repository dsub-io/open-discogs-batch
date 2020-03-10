package io.dsub.dumpdbmgmt.entity.intermed;

import io.dsub.dumpdbmgmt.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Document entity for MongoDB to represent
 * relationship between release and artist who
 * recorded as "Credited".
 */

@Getter
@AllArgsConstructor
@With
@Document(collection = "artist_credit")
public final class ArtistCredit extends BaseEntity {
    @Id
    private final ObjectId id;
    @Field(name = "artist_id")
    private final Long artist;
    @Field(name = "release_id")
    private final Long release;
    @Field(name = "credit")
    private final String credit;

    public ArtistCredit() {
        this.id = null;
        this.artist = null;
        this.release = null;
        this.credit = null;
    }
}
