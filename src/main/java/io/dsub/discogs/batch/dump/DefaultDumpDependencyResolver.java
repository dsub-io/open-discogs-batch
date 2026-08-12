package io.dsub.discogs.batch.dump;

import io.dsub.discogs.batch.argument.ArgType;
import io.dsub.discogs.batch.dump.service.DiscogsDumpService;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.time.YearMonth;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultDumpDependencyResolver implements DumpDependencyResolver {

  private static final String ENTITIES = ArgType.TYPE.getGlobalName();
  private static final String DUMP_MONTH = ArgType.DUMP_MONTH.getGlobalName();

  private final DiscogsDumpService dumpService;

  @Override
  public Collection<DiscogsDump> resolve(ApplicationArguments args)
      throws DumpNotFoundException, InvalidArgumentException {
    Set<EntityType> entities = parseEntities(args);
    if (!args.containsOption(DUMP_MONTH)) {
      return dumpService.resolveLatest(entities);
    }

    YearMonth dumpMonth = YearMonth.parse(args.getOptionValues(DUMP_MONTH).getFirst());
    return dumpService.resolveMonth(entities, dumpMonth);
  }

  protected Set<EntityType> parseEntities(ApplicationArguments args)
      throws InvalidArgumentException {
    if (!args.containsOption(ENTITIES)) {
      return EnumSet.allOf(EntityType.class);
    }

    EnumSet<EntityType> entities = EnumSet.noneOf(EntityType.class);
    for (String value : args.getOptionValues(ENTITIES)) {
      entities.add(EntityType.of(value));
    }
    if (entities.isEmpty()) {
      throw new InvalidArgumentException("entities cannot be empty");
    }
    return entities;
  }
}
