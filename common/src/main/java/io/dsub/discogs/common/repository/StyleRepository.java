package io.dsub.discogs.common.repository;

import io.dsub.discogs.common.entity.Style;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StyleRepository extends JpaRepository<Style, Long> {

  boolean existsByName(String name);

  Long findByName(String name);
}
