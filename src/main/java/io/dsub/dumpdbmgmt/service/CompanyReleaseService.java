package io.dsub.dumpdbmgmt.service;

import io.dsub.dumpdbmgmt.entity.intermed.CompanyRelease;
import io.dsub.dumpdbmgmt.repository.CompanyReleaseRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CompanyReleaseService {
    private CompanyReleaseRepository repository;

    public CompanyReleaseService(CompanyReleaseRepository repository) {
        this.repository = repository;
    }

    public CompanyRelease save(CompanyRelease companyRelease) {
        return this.repository.save(companyRelease);
    }

    public Iterable<CompanyRelease> saveAll(CompanyRelease... companyReleases) {
        return this.repository.saveAll(Arrays.asList(companyReleases));
    }


    public void delete(CompanyRelease companyRelease) {
        this.repository.delete(companyRelease);
    }

    public void deleteAll(CompanyRelease... companyReleases) {
        this.repository.deleteAll(Arrays.asList(companyReleases));
    }
}
