package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntityTypeUnitTest {

  @Test
  void shouldResolveNamesCaseInsensitively() throws InvalidArgumentException {
    for (EntityType type : EntityType.values()) {
      assertThat(EntityType.of(type.name())).isEqualTo(type);
      assertThat(type.toString()).isEqualTo(type.name().toLowerCase());
    }
  }

  @Test
  void shouldRejectUnknownName() {
    assertThatThrownBy(() -> EntityType.of("unknown"))
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessage("failed to figure out type: unknown");
  }

  @Test
  void shouldResolveImportDependencies() {
    assertThat(EntityType.ARTIST.getDependencies()).containsExactly(EntityType.ARTIST);
    assertThat(EntityType.LABEL.getDependencies()).containsExactly(EntityType.LABEL);
    assertThat(EntityType.MASTER.getDependencies())
        .containsExactly(EntityType.ARTIST, EntityType.LABEL, EntityType.MASTER);
    assertThat(EntityType.RELEASE.getDependencies()).containsExactly(EntityType.values());
    assertThat(EntityType.RELEASE.getDependencies()).isEqualTo(List.of(EntityType.values()));
  }
}
