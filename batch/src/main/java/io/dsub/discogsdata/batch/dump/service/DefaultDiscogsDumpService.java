package io.dsub.discogsdata.batch.dump.service;

import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpSupplier;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.batch.dump.repository.DiscogsDumpRepository;
import io.dsub.discogsdata.common.exception.DumpNotFoundException;
import io.dsub.discogsdata.common.exception.InitializationFailureException;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDiscogsDumpService implements DiscogsDumpService, InitializingBean {

  // the initial dump registered to the db is from March 8, 2008.
  // check out the following web directory: data.discogs.com
  public static final LocalDate FIRST_DUMP_YEAR_MONTH = LocalDate.of(2008, 3, 1);
  private final DiscogsDumpRepository repository;
  private final DumpSupplier dumpSupplier;

  /**
   * Fetches the entire list of discogs dump from the html page, then persist to the database via
   * repository.
   *
   * @see <a href="https://data.discogs.com">https://data.discogs.com</a>
   */
  @Override
  public void updateDB() {
    List<DiscogsDump> dumpList = dumpSupplier.get();
    if (dumpList == null || dumpList.isEmpty()) {
      log.error("failed to fetch items via DumpSupplier. cancelling the update...");
      return;
    }
    repository.saveAll(dumpList.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }

  /**
   * Fetch single dump by ETag. As ETag is a primary key in the database, the uniqueness is
   * ascertained.
   *
   * @param eTag ETag of target dump to get.
   * @return {@link DiscogsDump} if found, otherwise null.
   */
  @Override
  public DiscogsDump getDiscogsDump(String eTag) throws InvalidArgumentException {
    return repository.findByeTag(eTag);
  }

  /**
   * Fetch most recent dump by {@link DumpType}.
   *
   * @param type A type to find.
   * @return Most recent dump of given type, or null if there is no dump of given type exists.
   */
  @Override
  public DiscogsDump getMostRecentDiscogsDumpByType(DumpType type) {
    return repository.findTopByTypeOrderByCreatedAtDesc(type);
  }

  /**
   * Fetch dump that matches to given type, given year and month.
   *
   * @param type A type of dump to be found.
   * @param year A target year.
   * @param month A target month.
   * @return {@link DiscogsDump} if found, otherwise null.
   */
  @Override
  public List<DiscogsDump> getDumpByTypeInRange(DumpType type, int year, int month) {
    LocalDate start = LocalDate.of(year, month, 1);
    LocalDate end = start.plusMonths(1).minusDays(1);
    return repository.findByTypeAndCreatedAtBetween(type, start, end);
  }

  /**
   * Fetch latest complete dump that contains all four {@link DumpType} from the same year and
   * month. It is extremely important to note that the current year and month must be the same from
   * those of UTC.
   *
   * @return latest complete dump set.
   */
  @Override
  public List<DiscogsDump> getLatestCompleteDumpSet() throws DumpNotFoundException {
    LocalDate curr = LocalDate.now(Clock.systemUTC()); // get current date by timezone: UTC.

    int year = curr.getYear(), month = curr.getMonthValue();

    LocalDate start = LocalDate.of(year, month, 1);
    LocalDate end = LocalDate.of(year, month, 1).plusMonths(1);

    // limit condition to first known year and month.
    while (start.isAfter(FIRST_DUMP_YEAR_MONTH)) {
      // first.. count the existing rows between the start and end date.
      int count = repository.countAllByCreatedAtIsBetween(start, end);

      // count < 4 means that if we have a missing piece... i.e. artist is missing.
      if (count < 4) {
        start = start.minusMonths(1);
        end = end.minusMonths(1);
        continue;
      }

      return repository.findAllByCreatedAtIsBetween(start, end).stream()
          .sorted(DiscogsDump::compareTo)
          .limit(4)
          .collect(Collectors.toList());
    }

    throw new DumpNotFoundException("failed to locate the complete dump set...");
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
