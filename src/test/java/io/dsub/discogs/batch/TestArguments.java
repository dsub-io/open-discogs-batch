package io.dsub.discogs.batch;

import io.dsub.discogs.batch.domain.artist.ArtistSubItemsXML;
import io.dsub.discogs.batch.domain.artist.ArtistXML;
import io.dsub.discogs.batch.domain.label.LabelSubItemsXML;
import io.dsub.discogs.batch.domain.label.LabelXML;
import io.dsub.discogs.batch.domain.master.MasterSubItemsXML;
import io.dsub.discogs.batch.domain.master.MasterXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsXML;
import io.dsub.discogs.batch.domain.release.ReleaseItemXML;
import io.dsub.discogs.batch.dump.DiscogsDump;
import io.dsub.discogs.batch.dump.EntityType;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;

@Slf4j
public class TestArguments {

  public static final String DOMAIN_PKG = "io.dsub.discogs.batch.domain";
  public static final String BASE_XML_PATH = "src/test/resources/test/reader";
  public static final String ARTIST = "artist";
  public static final String LABEL = "label";
  public static final String MASTER = "master";
  public static final String RELEASE = "release";
  public static final Random RAND = new Random();

  public static final List<Class<?>> XML_CLASSES;

  static {
    try (ScanResult scanResult = new ClassGraph()
        .enableAllInfo()
        .acceptPackages(DOMAIN_PKG)
        .scan()) {
      XML_CLASSES = scanResult.getAllClasses().stream()
          .filter(classInfo -> !classInfo.isAbstract())
          .map(ClassInfo::loadClass)
          .collect(Collectors.toList());
    }
  }

  public static Stream<Class<?>> getXmlClasses() {
    return XML_CLASSES.stream();
  }

  public static Stream<String> coreEntityNames() {
    return List.of(ARTIST, LABEL, MASTER, RELEASE).stream();
  }

  public static Stream<Path> xmlPaths() {
    return coreEntityNames().map(item -> Path.of(BASE_XML_PATH, item + ".xml.gz"));
  }

  public static Stream<ItemReaderTestArgument> itemReaderTestArguments() {
    return List.of(
        new ItemReaderTestArgument(ArtistXML.class, "artist"),
        new ItemReaderTestArgument(ArtistSubItemsXML.class, "artist"),
        new ItemReaderTestArgument(LabelXML.class, "label"),
        new ItemReaderTestArgument(LabelSubItemsXML.class, "label"),
        new ItemReaderTestArgument(MasterXML.class, "master"),
        new ItemReaderTestArgument(MasterSubItemsXML.class, "master"),
        new ItemReaderTestArgument(ReleaseItemXML.class, "release"),
        new ItemReaderTestArgument(ReleaseItemSubItemsXML.class, "release")).stream();
  }

  public static LocalDate getLocalDateFrom(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

  public static DiscogsDump getRandomDump() {
    return getRandomDumpWithType(getRandomType());
  }

  public static DiscogsDump getRandomDumpWithType(EntityType type) {
    LocalDate lastModifiedAt = LocalDate.now(Clock.systemUTC())
        .minusYears(RAND.nextInt(10))
        .minusDays(RAND.nextInt(10))
        .minusMonths(RAND.nextInt(19));
    return getRandomDumpWithType(type, lastModifiedAt);
  }

  public static DiscogsDump getRandomDumpWithLastModifiedAt(LocalDate lastModifiedAt) {
    String etag = RandomString.make(19);
    long size = RAND.nextLong();
    String uriString = RandomString.make(30);
    EntityType type = getRandomType();
    return new DiscogsDump(etag, type, uriString, size, lastModifiedAt, null);
  }

  public static DiscogsDump getRandomDumpWithType(EntityType type, LocalDate lastModifiedAt) {
    String etag = RandomString.make(19);
    long size = RAND.nextLong();
    String uriString = RandomString.make(30);
    return new DiscogsDump(etag, type, uriString, size, lastModifiedAt, null);
  }

  public static Stream<LocalDate> getLocalDateTimes() {
    LocalDate ldt = LocalDate.of(2016, 1, 1);
    List<LocalDate> list = new ArrayList<>();
    while (ldt.isBefore(LocalDate.now())) {
      list.add(ldt);
      ldt = ldt.plusMonths(1);
    }
    return list.stream();
  }

  public static EntityType getRandomType() {
    return EntityType.values()[RAND.nextInt(4)];
  }

  @Data
  public static class ItemReaderTestArgument {

    private Class<?> mappedClass;
    private String rootElementName;
    private Path xmlPath;

    public ItemReaderTestArgument(Class<?> mappedClass, String rootElementName) {
      this.mappedClass = mappedClass;
      this.rootElementName = rootElementName;
      this.xmlPath = Path.of(BASE_XML_PATH, rootElementName + ".xml.gz");
    }
  }
}