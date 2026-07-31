package io.dsub.discogs.batch.dump.service;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import io.dsub.discogs.batch.dump.repository.DiscogsDumpRepository;
import io.dsub.discogs.batch.exception.DumpNotFoundException;
import io.dsub.discogs.batch.exception.InitializationFailureException;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultDiscogsDumpService implements DiscogsDumpService, InitializingBean {

  // the initial dump registered to the db is from March 8, 2008.
  // check out the following web directory: data.discogs.com
  public static final LocalDate FIRST_DUMP_YEAR_MONTH = LocalDate.of(2008, 3, 1);
  private final DiscogsDumpRepository repository;
  private final DumpSupplier dumpSupplier;

  public DefaultDiscogsDumpService(DiscogsDumpRepository repository, DumpSupplier dumpSupplier) {
    this.repository = repository;
    this.dumpSupplier = dumpSupplier;
  }

  /**
   * Fetches the entire list of discogs dump from the html page, then persist to the database via
   * repository.
   *
   * @see <a href="https://data.discogs.com">https://data.discogs.com</a>
   */
  @Override
  public void updateDB() {
    int monthlyDumpCount = repository.countItemsAfter(LocalDate.now().withDayOfMonth(1));

    if (monthlyDumpCount == 4) {
      log.info("repository is up to date. skipping the update...");
      return;
    }

    List<DiscogsDump> dumpList = dumpSupplier.get();

    if (dumpList == null || dumpList.isEmpty()) {
      log.error("failed to fetch items via DumpSupplier. cancelling the update...");
      return;
    }

    long persistedSize = repository.count();
    if (dumpList.size() == persistedSize) { // if already up-to-date.
      return;
    }

    log.info(
        "repository requires update. found {} items but repository has {} items.",
        persistedSize,
        dumpList.size());

    repository.saveAll(dumpList.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }

  @Override
  public boolean exists(String eTag) {
    return repository.existsByETag(eTag);
  }

  /**
   * Fetch single dump by ETag. As ETag is a primary key in the database, the uniqueness is
   * ascertained.
   *
   * @param eTag ETag of target dump to get.
   * @return {@link DiscogsDump} if found, otherwise null.
   */
  @Override
  public DiscogsDump getDiscogsDump(String eTag) throws DumpNotFoundException {
    return repository.findByETag(eTag);
  }

  /**
   * Fetch most recent dump by {@link EntityType}.
   *
   * @param type A type to find.
   * @return Most recent dump of given type, or null if there is no dump of given type exists.
   */
  @Override
  public DiscogsDump getMostRecentDiscogsDumpByType(EntityType type) {
    return repository.findTopByType(type);
  }

  /**
   * Fetch most recent dump by {@link EntityType}, year and month.
   *
   * @param type A type to find.
   * @return Most recent dump from given type, year and month or null if there is no dump of given
   * type exists.
   */
  @Override
  public DiscogsDump getMostRecentDiscogsDumpByTypeYearMonth(EntityType type, int year, int month) {
    LocalDate start = LocalDate.of(year, month, 1);
    return repository.findTopByTypeAndLastModifiedAtBetween(
        type, start, start.plusMonths(1));
  }

  /**
   * Fetches collection of {@link DiscogsDump} from given type, year and month. The types must be
   * unique and each type must be present during the requested month (otherwise throws).
   *
   * <p>If a month contains multiple publications, this returns the newest dump independently for
   * each requested type. The selected types may therefore have different dump dates.
   *
   * @param types target {@link EntityType}(s). throws {@link InvalidArgumentException} if null or
   *              blank.
   * @param year  year to search for.
   * @param month month to search for.
   * @return Collection of {@link DiscogsDump} found from criteria.
   * @throws InvalidArgumentException thrown if argument contains duplicated entry.
   * @throws DumpNotFoundException    thrown if given type, year and month cannot be found.
   */
  @Override
  public Collection<DiscogsDump> getAllByTypeYearMonth(List<EntityType> types, int year, int month)
      throws DumpNotFoundException {
    Set<EntityType> requiredTypes = new LinkedHashSet<>(types);
    LocalDate targetDate = LocalDate.of(year, month, 1);
    List<DiscogsDump> dumpList =
        findLatestPerType(
            requiredTypes,
            repository.findAllByLastModifiedAtIsBetween(
                targetDate, targetDate.plusMonths(1)));
    if (!dumpList.isEmpty()) {
      return dumpList;
    }

    LocalDate now = LocalDate.now();
    if (now.getYear() == year && now.getMonthValue() == month) {
      throw new DumpNotFoundException(
          "dump for current month seems to be missing from distribution.");
    }
    if (requiredTypes.size() == 1) {
      throw new DumpNotFoundException(
          "dump of type "
              + requiredTypes.iterator().next()
              + " from "
              + year
              + "-"
              + month
              + " not found");
    }
    throw new DumpNotFoundException(
        "dump set of types "
            + requiredTypes
            + " from "
            + year
            + "-"
            + month
            + " not found");
  }

  /**
   * Fetch dump that matches to given type, given year and month.
   *
   * @param type  A type of dump to be found.
   * @param year  A target year.
   * @param month A target month.
   * @return {@link DiscogsDump} if found, otherwise null.
   */
  @Override
  public List<DiscogsDump> getDumpByTypeInRange(EntityType type, int year, int month) {
    LocalDate start = LocalDate.of(year, month, 1);
    LocalDate end = start.plusMonths(1);
    return repository.findByTypeAndLastModifiedAtBetween(type, start, end);
  }

  /**
   * Fetch the latest available dump independently for every {@link EntityType}. Dates may differ
   * when an upstream publication omits one domain.
   *
   * @return latest complete dump set.
   */
  @Override
  public List<DiscogsDump> getLatestCompleteDumpSet() throws DumpNotFoundException {
    List<DiscogsDump> latest =
        findLatestPerType(Set.of(EntityType.values()), repository.findAll());
    if (!latest.isEmpty()) {
      return latest;
    }
    throw new DumpNotFoundException("failed to locate a dump for every entity type");
  }

  /**
   * Fetch entire dump as a list.
   *
   * @return entire dump list.
   */
  @Override
  public List<DiscogsDump> getAll() {
    return repository.findAll();
  }

  private List<DiscogsDump> findLatestPerType(
      Set<EntityType> requiredTypes, Collection<DiscogsDump> candidates) {
    if (requiredTypes.isEmpty()) {
      return List.of();
    }

    Map<EntityType, DiscogsDump> latest = new EnumMap<>(EntityType.class);
    candidates.stream()
        .filter(Objects::nonNull)
        .filter(dump -> requiredTypes.contains(dump.getType()))
        .forEach(
            dump ->
                latest.merge(
                    dump.getType(),
                    dump,
                    (left, right) -> left.compareTo(right) >= 0 ? left : right));

    if (!latest.keySet().containsAll(requiredTypes)) {
      return List.of();
    }
    return requiredTypes.stream()
        .sorted(java.util.Comparator.comparingInt(Enum::ordinal))
        .map(latest::get)
        .collect(Collectors.toList());
  }

  /**
   * Implementation of {@link InitializingBean} interface.
   *
   * @throws InitializationFailureException if either repository or dumpSupplier is null.
   */
  @Override
  public void afterPropertiesSet() throws InitializationFailureException {
    if (this.repository == null) {
      throw new InitializationFailureException("repository cannot be null");
    }
    if (this.dumpSupplier == null) {
      throw new InitializationFailureException("dumpSupplier cannot be null");
    }
  }
}
