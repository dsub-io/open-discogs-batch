package io.dsub.discogs.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ReflectionUtilTest {

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityClass__WhenInvokeNoArgConstructor_ShouldSuccessfullyGenerate(
      Class<?> entityClass) {
    // when
    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);

    // then
    assertThat(instance).isNotNull().isExactlyInstanceOf(entityClass);

    assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true)).isEmpty();
  }

  @Test
  void givenClassDoesNotHaveNoArgConstructor__WhenInvokeNoArgConstructor_ShouldThrow() {
    class TestClass {

      final int value;

      public TestClass(int value) {
        this.value = value;
      }
    }

    // when
    Throwable t = catchThrowable(() -> ReflectionUtil.invokeNoArgConstructor(TestClass.class));

    // then

    assertThat(t).isNotNull().hasMessage("TestClass does not have no-arg constructor");
  }

  @Test
  void
  givenConstructorThrowsIllegalAccessException__WhenInvokeNoArgConstructor_LogThenReturnNull() {
    // when
    Object o = ReflectionUtil.invokeNoArgConstructor(ExceptionThrowingTestClass.class);

    // then
    assertThat(o).isEqualTo(null);
    List<String> logs = logSpy.getLogsByExactLevelAsString(Level.WARN, true);
    assertThat(logs).isNotEmpty();
    String log = logs.get(0);
    assertThat(log)
        .isEqualTo("failed to instantiate " + ExceptionThrowingTestClass.class.getSimpleName());
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityClass__WhenGetDeclaredFields_ShouldReturnAfterSetAccessibleToTrue(
      Class<?> entityClass) {

    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);
    assertThat(instance).isNotNull();

    // when
    List<Field> fields = ReflectionUtil.getDeclaredFields(instance);

    // then
    for (Field field : fields) {
      assertDoesNotThrow(() -> ReflectionUtil.setFieldValue(instance, field, null));
    }
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityClass__WhenGetDeclaredFields_ShouldNotReturnStaticOrFinalFields(
      Class<?> entityClass) {

    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);
    assertThat(instance).isNotNull();

    // when
    List<Field> fields = ReflectionUtil.getDeclaredFields(instance);

    // then
    for (Field field : fields) {
      assertThat(Modifier.isStatic(field.getModifiers())).isFalse();
      assertThat(Modifier.isFinal(field.getModifiers())).isFalse();
    }
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenEntityClass__WhenGetFields__ShouldReturnStringFields(Class<?> entityClass) {
    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);
    assertThat(instance).isNotNull();

    // when
    List<Field> fields =
        ReflectionUtil.getDeclaredFields(instance, field -> field.getType().equals(String.class));

    // then
    for (Field field : fields) {
      assertThat(field.getType()).isEqualTo(String.class);
    }
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenFieldDoesNotMatchValueType__WhenSetFieldValue__ShouldThrow(Class<?> entityClass) {
    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);
    assertThat(instance).isNotNull();

    // when
    List<Field> fields =
        ReflectionUtil.getDeclaredFields(instance, field -> !field.getType().equals(String.class));

    // then
    for (Field field : fields) {
      Throwable t =
          catchThrowable(() -> ReflectionUtil.setFieldValue(instance, field, RandomString.make()));
      assertThat(t)
          .isNotNull()
          .hasMessage(
              "fieldType "
                  + field.getType().getSimpleName()
                  + " does not match "
                  + String.class.getSimpleName());
    }
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenFieldOrInstanceIsNull__WhenSetFieldValue__ShouldThrow(Class<?> entityClass) {
    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);
    assertThat(instance).isNotNull();

    // given
    Field nullField = null;
    Field field = ReflectionUtil.getDeclaredFields(instance).get(0);
    // when
    Throwable t1 = catchThrowable(() -> ReflectionUtil.setFieldValue(instance, nullField, null));
    Throwable t2 = catchThrowable(() -> ReflectionUtil.setFieldValue(null, field, null));

    // then
    assertThat(t1)
        .isInstanceOf(InvalidArgumentException.class)
        .isNotNull()
        .hasMessage("field cannot be null");

    assertThat(t2)
        .isInstanceOf(InvalidArgumentException.class)
        .isNotNull()
        .hasMessage("target object cannot be null");
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenFieldOrInstanceIsNull__GetValue__ShouldThrow(Class<?> entityClass) {
    // given
    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);
    assertThat(instance).isNotNull();
    Field field = ReflectionUtil.getDeclaredFields(instance).get(0);

    // when
    Throwable t1 = catchThrowable(() -> ReflectionUtil.getValue(instance, null));
    Throwable t2 = catchThrowable(() -> ReflectionUtil.getValue(null, field));

    // then
    assertThat(t1)
        .isInstanceOf(InvalidArgumentException.class)
        .isNotNull()
        .hasMessage("field cannot be null");

    assertThat(t2)
        .isInstanceOf(InvalidArgumentException.class)
        .isNotNull()
        .hasMessage("target object cannot be null");
  }

  @ParameterizedTest
  @MethodSource("io.dsub.discogs.batch.TestArguments#entities")
  void givenInstanceHasValues__GetValue__ShouldReturnAsIs(Class<?> entityClass) {
    // given
    Object instance = ReflectionUtil.invokeNoArgConstructor(entityClass);
    assertThat(instance).isNotNull();
    List<Field> stringFields =
        ReflectionUtil.getDeclaredFields(instance, field -> field.getType().equals(String.class));
    stringFields.forEach(
        field -> ReflectionUtil.setFieldValue(instance, field, RandomString.make()));

    // when
    List<Object> values =
        stringFields.stream()
            .map(field -> ReflectionUtil.getValue(instance, field))
            .collect(Collectors.toList());

    // then
    values.forEach(value -> assertThat(value).isNotNull().isExactlyInstanceOf(String.class));
  }

  @Test
  void givenInstanceHasBlankString__WhenNormalizeString__ShouldTreatProperly() {

    Supplier<TestChild> testChildSupplier =
        () -> {
          TestChild testChild = new TestChild();
          List<String> list = new ArrayList<>();
          list.add("");
          testChild.list = list;
          testChild.firstVal = "";
          testChild.secondVal = "world";
          return testChild;
        };

    // given
    TestParent testParent = new TestParent();
    testParent.firstVal = "";
    testParent.secondVal = "hello";
    testParent.testChild = testChildSupplier.get();
    testParent.testChild.list.add("hello");
    List<TestChild> subItems = new ArrayList<>();
    IntStream.range(0, 5).mapToObj(i -> testChildSupplier.get()).forEach(subItems::add);
    testParent.subItems = subItems;

    // when
    ReflectionUtil.normalizeStringFields(testParent);

    // then
    assertThat(testParent.firstVal).isNull();
    assertThat(testParent.secondVal).isEqualTo("hello");
    assertThat(testParent.testChild.list).hasSize(1);
    assertThat(testParent.testChild.firstVal).isEqualTo(null);
    assertThat(testParent.testChild.secondVal).isEqualTo("world");
    assertThat(testParent.secondVal).isEqualTo("hello");

    subItems.forEach(
        child -> {
          assertThat(child.firstVal).isNull();
          assertThat(child.secondVal).isEqualTo("world");
          assertThat(child.list).isNull();
        });
  }

  private static class ExceptionThrowingTestClass {

    public ExceptionThrowingTestClass() {
      throw new RuntimeException("test");
    }
  }

  private static class TestParent {

    String firstVal;
    String secondVal;
    List<TestChild> subItems;
    TestChild testChild;
  }

  private static class TestChild {

    List<String> list;
    String firstVal;
    String secondVal;
  }
}
