package io.dsub.discogs.batch.dump.repository;

import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.DumpSupplier;
import io.dsub.discogs.batch.dump.EntityType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository
@RequiredArgsConstructor
public class MapDiscogsDumpRepository implements DiscogsDumpRepository, InitializingBean {

  private final DumpSupplier dumpSupplier;
  private Set<DiscogsDump> cache;

  @Override
  public List<DiscogsDump> findAll() {
    return new ArrayList<>(cache);
  }

  @Override
  public DiscogsDump findByETag(String ETag) {
    return cache.stream().filter(dump -> dump.getETag().equals(ETag)).findFirst().orElse(null);
  }

  @Override
  public boolean existsByETag(String ETag) {
    return cache.stream().anyMatch(dump -> dump.getETag().equals(ETag));
  }

  @Override
  public int count() {
    return (int) cache.stream().count();
  }

  @Override
  public void saveAll(Collection<DiscogsDump> discogsDumps) {
    if (discogsDumps == null || discogsDumps.isEmpty()) {
      return;
    }
    cache.addAll(discogsDumps);
  }

  @Override
  public void deleteAll() {
    cache.clear();
  }

  @Override
  public int countItemsAfter(LocalDate start) {
    return (int) cache.stream().filter(dump -> isDumpEqualOrAfter(dump, start)).count();
  }

  @Override
  public int countItemsBefore(LocalDate end) {
    return (int) cache.stream().filter(dump -> isDumpEqualOrBefore(dump, end)).count();
  }

  @Override
  public int countItemsBetween(LocalDate start, LocalDate end) {
    return (int) cache.stream().filter(dump -> isDumpEqualOrBetween(dump, start, end)).count();
  }

  @Override
  public List<DiscogsDump> findByTypeAndLastModifiedAtBetween(
      EntityType type, LocalDate start, LocalDate end) {
    return cache.stream()
        .filter(dump -> dump.getType().equals(type))
        .filter(dump -> isDumpEqualOrBetween(dump, start, end))
        .collect(Collectors.toList());
  }

  @Override
  public DiscogsDump findTopByTypeAndLastModifiedAtBetween(
      EntityType type, LocalDate start, LocalDate end) {
    return cache.stream()
        .filter(dump -> dump.getType().equals(type))
        .filter(dump -> isDumpEqualOrBetween(dump, start, end))
        .max(DiscogsDump::compareTo)
        .orElse(null);
  }

  @Override
  public DiscogsDump findTopByType(EntityType type) {
    return cache.stream()
        .filter(dump -> dump.getType().equals(type))
        .max(DiscogsDump::compareTo)
        .orElse(null);
  }

  @Override
  public List<DiscogsDump> findAllByLastModifiedAtIsBetween(LocalDate start, LocalDate end) {
    return cache.stream()
        .filter(dump -> isDumpEqualOrBetween(dump, start, end))
        .collect(Collectors.toList());
  }

  @Override
  public void save(DiscogsDump dump) {
    cache.add(dump);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(dumpSupplier, "dumpSupplier cannot be null");
    List<DiscogsDump> dumpList = dumpSupplier.get();
    cache = new ConcurrentSkipListSet<>(dumpList);
  }

  private boolean isDumpEqualOrBetween(DiscogsDump dump, LocalDate start, LocalDate end) {
    return isDumpEqualOrAfter(dump, start) && dump.getLastModifiedAt().isBefore(end);
  }

  private boolean isDumpEqualOrAfter(DiscogsDump dump, LocalDate start) {
    LocalDate ldt = dump.getLastModifiedAt();
    return ldt.isAfter(start) || ldt.isEqual(start);
  }

  private boolean isDumpEqualOrBefore(DiscogsDump dump, LocalDate end) {
    LocalDate ldt = dump.getLastModifiedAt();
    return ldt.isBefore(end) || ldt.isEqual(end);
  }
}
