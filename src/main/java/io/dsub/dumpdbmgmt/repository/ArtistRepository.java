package io.dsub.dumpdbmgmt.repository;

import io.dsub.dumpdbmgmt.entity.Artist;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ArtistRepository extends MongoRepository<Artist, Long> {
}
