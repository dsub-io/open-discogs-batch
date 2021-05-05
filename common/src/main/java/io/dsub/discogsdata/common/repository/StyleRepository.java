package io.dsub.discogsdata.common.repository;

import io.dsub.discogsdata.common.entity.Style;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StyleRepository extends JpaRepository<Style, Long> {
  boolean existsByName(String name);

  Long findByName(String name);
}
