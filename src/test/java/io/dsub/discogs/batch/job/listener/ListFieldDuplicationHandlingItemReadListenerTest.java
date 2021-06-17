package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListFieldDuplicationHandlingItemReadListenerTest {

  @Getter
  @Setter
  class TestClass {
    private List<String> stringList;
    private List<Object> objList;
  }

  TestClass testObj;

  ListFieldDuplicationHandlingItemReadListener<TestClass> listener =
      new ListFieldDuplicationHandlingItemReadListener<>();

  @BeforeEach
  void setUp() {
    testObj = new TestClass();
  }

  @Test
  void givenItemHasStringList__WhenAfterRead__ShouldMakeUniqueList() {
    int expectedSize = 10;
    List<String> strings =
        IntStream.range(0,10).mapToObj(i -> RandomString.make()).collect(Collectors.toList());
    strings.add(null);
    strings.addAll(strings.stream().limit(5).collect(Collectors.toList()));

    // given
    testObj.setObjList(strings.stream().map(s -> (Object) s).collect(Collectors.toList()));
    testObj.setStringList(strings);

    // when
    listener.afterRead(testObj);

    // then
    assertThat(testObj.getObjList()).hasSize(expectedSize);
    assertThat(testObj.getStringList()).hasSize(expectedSize);
  }

  @Test
  void givenItemHasNullFields__WhenAfterRead__ShouldNotThrow() {
    // given
    assertThat(testObj.getObjList()).isNull();
    assertThat(testObj.getStringList()).isNull();

    // when
    Throwable t = catchThrowable(() -> listener.afterRead(testObj));

    // then
    assertThat(t).isNull();
  }
}
