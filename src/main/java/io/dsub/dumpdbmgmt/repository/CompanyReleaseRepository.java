package io.dsub.dumpdbmgmt.repository;

import io.dsub.dumpdbmgmt.entity.intermed.CompanyRelease;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CompanyReleaseRepository extends MongoRepository<CompanyRelease, Long> {
}
