package io.dsub.discogs.batch.job.registry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultEntityIdRegistryUnitTest {

  @Test
  void integerIdentifiersRejectInvalidInputsAndStoreValidValues() {
    DefaultEntityIdRegistry registry = new DefaultEntityIdRegistry();

    assertThat(registry.exists(EntityIdRegistry.Type.ARTIST, (Integer) null)).isFalse();
    assertThat(registry.exists(EntityIdRegistry.Type.ARTIST, 0)).isFalse();
    registry.put(null, 1);
    registry.put(EntityIdRegistry.Type.ARTIST, (Integer) null);
    registry.put(EntityIdRegistry.Type.ARTIST, 1);

    assertThat(registry.exists(EntityIdRegistry.Type.ARTIST, 1)).isTrue();
  }

  @Test
  void stringIdentifiersRejectInvalidInputsAndStoreValidValues() {
    DefaultEntityIdRegistry registry = new DefaultEntityIdRegistry();

    registry.put(EntityIdRegistry.Type.GENRE, (String) null);
    registry.put(EntityIdRegistry.Type.GENRE, " ");
    registry.put(EntityIdRegistry.Type.GENRE, "Rock");
    registry.put(EntityIdRegistry.Type.STYLE, "House");

    assertThat(registry.exists(EntityIdRegistry.Type.GENRE, "Rock")).isTrue();
    assertThat(registry.exists(EntityIdRegistry.Type.GENRE, " ")).isFalse();
    assertThat(registry.exists(EntityIdRegistry.Type.STYLE, "House")).isTrue();
  }

  @Test
  void clearAllRemovesEveryIdentifierType() {
    DefaultEntityIdRegistry registry = new DefaultEntityIdRegistry();
    for (EntityIdRegistry.Type type :
        new EntityIdRegistry.Type[] {
          EntityIdRegistry.Type.ARTIST,
          EntityIdRegistry.Type.LABEL,
          EntityIdRegistry.Type.MASTER,
          EntityIdRegistry.Type.RELEASE
        }) {
      registry.put(type, 1);
      assertThat(registry.getLongIdCache(type).getType()).isEqualTo(type);
    }
    registry.put(EntityIdRegistry.Type.GENRE, "Rock");
    registry.put(EntityIdRegistry.Type.STYLE, "House");

    registry.clearAll();

    assertThat(registry.exists(EntityIdRegistry.Type.ARTIST, 1)).isFalse();
    assertThat(registry.exists(EntityIdRegistry.Type.LABEL, 1)).isFalse();
    assertThat(registry.exists(EntityIdRegistry.Type.MASTER, 1)).isFalse();
    assertThat(registry.exists(EntityIdRegistry.Type.RELEASE, 1)).isFalse();
    assertThat(registry.exists(EntityIdRegistry.Type.GENRE, "Rock")).isFalse();
    assertThat(registry.exists(EntityIdRegistry.Type.STYLE, "House")).isFalse();
  }
}
