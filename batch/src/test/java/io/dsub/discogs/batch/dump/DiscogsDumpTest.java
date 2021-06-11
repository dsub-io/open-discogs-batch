package io.dsub.discogs.batch.dump;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiscogsDumpTest {

  @BeforeEach
  void setUp() {}

  @Test
  void whenCompared__ShouldDelegateComparison__ToLocalDate() {
    LocalDate priorDate = LocalDate.of(2000, 1, 1);
    LocalDate latterDate = LocalDate.of(2001, 1, 1);
    DiscogsDump prior = getDiscogsDumpWithCreatedAt(priorDate);
    DiscogsDump latter = getDiscogsDumpWithCreatedAt(latterDate);
    assertThat(prior.compareTo(latter)).isEqualTo(priorDate.compareTo(latterDate));
    assertThat(latter.compareTo(prior)).isEqualTo(latterDate.compareTo(priorDate));
    latter.setCreatedAt(priorDate);
    assertThat(prior.compareTo(latter)).isEqualTo(0);
  }

  @Test
  void whenGetClone__ShouldReturnUniqueInstanceWith__SameValuesWithin() {
    DiscogsDump toTest = getDiscogsDumpWithCreatedAt(LocalDate.now());
    assertThat(toTest.getClone())
        .satisfies(clone -> assertThat(clone.getSize()).isEqualTo(toTest.getSize()))
        .satisfies(clone -> assertThat(clone.getCreatedAt()).isEqualTo(toTest.getCreatedAt()))
        .satisfies(clone -> assertThat(clone.getType()).isEqualTo(toTest.getType()))
        .satisfies(clone -> assertThat(clone.getETag()).isEqualTo(toTest.getETag()))
        .satisfies(clone -> assertThat(clone.getUriString()).isEqualTo(toTest.getUriString()))
        .satisfies(clone -> assertThat(clone).isNotSameAs(toTest));
  }

  @Test
  void whenEqualsCalled__ThenShouldOnlyCompareETag() {
    DiscogsDump first = getDiscogsDump();
    DiscogsDump second = first.getClone();

    // when
    second.setETag("something else");
    assertThat(second.getETag()).isNotEqualTo(first.getETag()); // etag is not equal

    // then
    assertThat(second).isNotEqualTo(first);
  }

  @Test
  void whenEverythingIsDifferent__ButETag__ThenShouldIdentifyAsSame() {
    // when
    DiscogsDump first = getDiscogsDump();
    DiscogsDump second = getDiscogsDump();

    second.setType(getDifferentTypeThan(first.getType()));

    second.setETag(first.getETag());
    second.setSize(first.getSize() + 10);
    second.setCreatedAt(first.getCreatedAt().plusMonths(1));
    assertThat(first)
        .satisfies(firstOne -> assertThat(firstOne.getType()).isNotEqualTo(second.getType()))
        .satisfies(firstOne -> assertThat(firstOne.getSize()).isNotEqualTo(second.getSize()))
        .satisfies(
            firstOne -> assertThat(firstOne.getCreatedAt()).isNotEqualTo(second.getCreatedAt()))
        .satisfies(
            firstOne -> assertThat(firstOne.getUriString()).isNotEqualTo(second.getUriString()))
        .satisfies(firstOne -> assertThat(firstOne.getETag()).isEqualTo(second.getETag()));

    // then
    assertThat(first).isEqualTo(second);
  }

  @Test
  void whenHashCodeCalled__ThenShouldReturnUniqueHashCode() {
    DiscogsDump first = getDiscogsDump();
    DiscogsDump second = getDiscogsDump();

    // when
    int firstHash = first.hashCode();
    int secondHash = second.hashCode();

    // then
    assertThat(firstHash).isNotEqualTo(secondHash);
  }

  @Test
  void whenHashCodeCalled__ThenShouldAlwaysReturnConsistentResult() {
    DiscogsDump discogsDump = getDiscogsDump();

    // when
    int first = discogsDump.hashCode();
    int second = discogsDump.hashCode();

    // then
    assertThat(first).isEqualTo(second);
  }

  @Test
  void whenGetCloneCalled__ThenShouldNotThrow() {
    Assertions.assertDoesNotThrow(() -> getDiscogsDump().getClone());
  }

  @Test
  void whenGetCloneCalled__ShouldReturnUniqueInstance__WithSameValue() {
    DiscogsDump origin = getDiscogsDump();

    // when
    DiscogsDump cloneDump = origin.getClone();

    // then
    assertThat(cloneDump)
        .satisfies(clone -> assertThat(clone.getETag()).isEqualTo(origin.getETag()))
        .satisfies(clone -> assertThat(clone.getSize()).isEqualTo(origin.getSize()))
        .satisfies(clone -> assertThat(clone.getCreatedAt()).isEqualTo(origin.getCreatedAt()))
        .satisfies(clone -> assertThat(clone.getType()).isEqualTo(origin.getType()))
        .satisfies(clone -> assertThat(clone.getUriString()).isEqualTo(origin.getUriString()));
  }

  @Test
  void whenGetFileNameWithExpectedUriFormat__ThenShouldReturnLastPartAsFileName() {
    DiscogsDump discogsDump = getDiscogsDump();
    discogsDump.setUriString("a/b/c");

    // when
    String fileName = discogsDump.getFileName();

    // then
    assertThat(fileName).isEqualTo("c");
  }

  @Test
  void whenGetFileNameCalledWhileURIStringIsNull__ThenShouldReturnNull() {
    DiscogsDump discogsDump = getDiscogsDump();

    // when
    discogsDump.setUriString(null);
    String fileName = discogsDump.getFileName();

    // when
    assertThat(fileName).isNull();
  }

  @Test
  void whenGetFileNameCalledWhileURIStringIsBlankString__ThenShouldReturnNull() {
    DiscogsDump discogsDump = getDiscogsDump();

    // when
    discogsDump.setUriString("");
    String fileName = discogsDump.getFileName();

    // then
    assertThat(fileName).isNull();
  }

  @Test
  void whenGetETagCalled__ThenShouldReturnCorrectValue() {
    String eTag = "hello etag";
    DiscogsDump discogsDump = getDiscogsDump();

    // when
    discogsDump.setETag(eTag);
    String result = discogsDump.getETag();

    // then
    assertThat(result).isEqualTo(eTag);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, 3})
  void whenGetTypeCalled__ThenShouldReturnMatchingType(int idx) {
    DiscogsDump discogsDump = getDiscogsDump();
    DumpType targetType = DumpType.values()[idx];
    discogsDump.setType(targetType);

    // when
    DumpType result = discogsDump.getType();

    // then
    assertThat(result).isEqualTo(targetType);
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", "", "3212", "random_string___", "hello_string"})
  void whenGetURIStringCalled__ShouldReturnMatchingURIString(String source) {
    DiscogsDump discogsDump = getDiscogsDump();
    discogsDump.setUriString(source);

    // when
    String result = discogsDump.getUriString();

    // then
    assertThat(result).isEqualTo(source);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 6, 34, 333, 574, 5432, 43, 42})
  void whenGetSizeCalled__ShouldReturnMatchingSizeValue(Integer source) {
    DiscogsDump discogsDump = getDiscogsDump();
    discogsDump.setSize(source.longValue());

    // when
    long result = discogsDump.getSize();

    // then
    assertThat(result).isEqualTo(source.longValue());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 6, 34, 333, 574, 5432, 43, 42})
  void whenGetCreatedAtCalled__ShouldReturnCorrectValue(int value) {
    Random random = new Random();
    DiscogsDump discogsDump = getDiscogsDump();
    LocalDate targetDate =
        LocalDate.of(random.nextInt(value), 1 + random.nextInt(12), 1 + random.nextInt(28));
    discogsDump.setCreatedAt(targetDate);

    // when
    LocalDate result = discogsDump.getCreatedAt();

    // then
    assertThat(result).isEqualTo(targetDate);
  }

  DiscogsDump getDiscogsDump() {
    return this.getDiscogsDumpWithCreatedAt(LocalDate.now());
  }

  DiscogsDump getDiscogsDumpWithCreatedAt(LocalDate createdAt) {
    return DiscogsDump.builder()
        .uriString(RandomString.make(10))
        .eTag(RandomString.make(10))
        .type(DumpType.values()[new Random().nextInt(4)])
        .size(10L)
        .createdAt(createdAt)
        .registeredAt(LocalDateTime.now().minusDays(new Random().nextInt(1000)))
        .build();
  }

  DumpType getDifferentTypeThan(DumpType that) {
    return Arrays.stream(DumpType.values())
        .filter(item -> !item.equals(that))
        .collect(Collectors.toList())
        .get(0);
  }
}
