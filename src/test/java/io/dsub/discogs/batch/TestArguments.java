package io.dsub.discogs.batch;

import io.dsub.discogs.common.entity.base.BaseEntity;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.reflections.Reflections;

public class TestArguments {

  public static final String BASE_PKG = "io.dsub.discogs";
  public static final String ENTITY_PKG = "io.dsub.discogs.common.entity";
  public static final String COMMAND_PKG = "io.dsub.discogs.batch.domain";
  public static final Reflections REFLECTIONS = new Reflections(BASE_PKG);
  public static final List<Class<? extends BaseEntity>> ENTITIES =
      REFLECTIONS.getSubTypesOf(BaseEntity.class).stream()
          .filter(clazz -> clazz.getPackageName().contains(ENTITY_PKG))
          .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
          .filter(clazz -> !Modifier.isInterface(clazz.getModifiers()))
          .collect(Collectors.toList());

  public static final List<Class<? extends BatchCommand>> COMMANDS =
      REFLECTIONS.getSubTypesOf(BatchCommand.class).stream()
          .filter(clazz -> clazz.getPackageName().contains(COMMAND_PKG))
          .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
          .filter(clazz -> !Modifier.isInterface(clazz.getModifiers()))
          .collect(Collectors.toList());

  public static Stream<Class<? extends BaseEntity>> entities() {
    return ENTITIES.stream();
  }

  public static Stream<Class<? extends BatchCommand>> commands() {
    return COMMANDS.stream();
  }
}
