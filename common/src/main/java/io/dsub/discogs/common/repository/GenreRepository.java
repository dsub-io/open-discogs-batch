package io.dsub.discogs.common.repository;

import io.dsub.discogs.common.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

  boolean existsByName(String name);

  Long findByName(String name);
}
