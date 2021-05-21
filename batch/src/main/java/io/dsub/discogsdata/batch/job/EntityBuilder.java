package io.dsub.discogsdata.batch.job;

import io.dsub.discogsdata.common.entity.base.BaseEntity;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EntityBuilder {

  private static final String ENTITY_PKG = "io.dsub.discogsdata.common.entity";

  private static final Map<Class<? extends BaseEntity>, BaseEntity> REGISTRY =
      new ConcurrentHashMap<>();

  public EntityBuilder() {
    init();
  }

  private void init() {
    Reflections reflections = new Reflections(ENTITY_PKG);
    reflections.getSubTypesOf(BaseEntity.class).stream()
        .filter(this::canRegister)
        .forEach(this::register);
  }

  private <T extends BaseEntity> boolean canRegister(Class<T> clazz) {
    int mod = clazz.getModifiers();
    return !Modifier.isInterface(mod) && !Modifier.isAbstract(mod);
  }

  @SuppressWarnings("unchecked")
  public <T extends BaseEntity> T register(Class<T> clazz) {
    if (REGISTRY.containsKey(clazz)) {
      log.info(clazz.getSimpleName() + " already registered.");
      return getInstance(clazz);
    }
    return tryInstantiate(clazz);
  }

  @SuppressWarnings("unchecked")
  public <T extends BaseEntity> T getInstance(Class<T> clazz) {
    if (REGISTRY.containsKey(clazz)) {
      return (T) REGISTRY.get(clazz);
    }
    return register(clazz);
  }

  private <T> T tryInstantiate(Class<T> clazz) {
    Constructor<T> constructor = null;
    try {
      constructor = clazz.getConstructor();
    } catch (NoSuchMethodException e) {
      throw new InitializationFailureException(
          "failed to locate no-arg constructor for class: " + clazz.getName());
    }
    try {
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (InstantiationException
        | InvocationTargetException
        | IllegalAccessException e) {
      throw new InitializationFailureException("failed to instantiate " + clazz.getSimpleName());
    }
  }
}
