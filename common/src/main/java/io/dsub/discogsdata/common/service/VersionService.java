package io.dsub.discogsdata.common.service;

import io.dsub.discogsdata.common.entity.Version;
import io.dsub.discogsdata.common.repository.VersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VersionService {
    private final VersionRepository versionRepository;

    public void saveVersion(Version version) {
        if (version.getId() > 0) {
            version.setId(0);
        }
        versionRepository.save(version);
    }

    public Version getCurrentVersion() {
        return versionRepository.findTopByOrderByIdDesc();
    }
}
