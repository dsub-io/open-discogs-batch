package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import io.dsub.discogs.common.entity.artist.Artist;
import java.lang.reflect.Field;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class StringFieldNormalizingItemReadListenerUnitTest {

  StringFieldNormalizingItemReadListener<Artist> listener =
      new StringFieldNormalizingItemReadListener<>();

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @Test
  void whenItemContainsBlank__ShouldSetSuchFieldToNull() {
    Artist artist =
        Artist.builder().realName("").name("").dataQuality("").profile("").id(1L).build();
    listener.afterRead(artist);
    for (Field field : artist.getClass().getDeclaredFields()) {
      field.setAccessible(true);
      if (!field.getType().equals(String.class)) {
        continue;
      }
      try {
        String value = (String) field.get(artist);
        assertThat(value).isNull();
      } catch (IllegalAccessException e) {
        fail(e);
      }
    }
  }

  @Test
  void whenNotBlankFieldExists__ShouldReturnAsIs() {
    Artist artist = Artist.builder().profile("hello world").build();

    listener.afterRead(artist);

    assertThat(artist.getProfile()).isNotNull().isEqualTo("hello world");
  }

  @Test
  void onFieldAccess__ShouldPassPrivateFinalField() {
    TestSubject testSubject = new TestSubject("any");

    StringFieldNormalizingItemReadListener<TestSubject> secondListener =
        new StringFieldNormalizingItemReadListener<>();

    // then
    assertDoesNotThrow(() -> secondListener.afterRead(testSubject));
  }

  @Test
  void whenOnReadError__ShouldProperlyLogTheExactMessage() {
    Exception e = new Exception("hell world");

    // when
    listener.onReadError(e);

    // then
    assertThat(logSpy.countExact(Level.ERROR)).isOne();
    assertThat(logSpy.getLogsByLevelAsString(Level.ERROR, true).get(0)).contains(e.getMessage());
  }

  @RequiredArgsConstructor
  static class TestSubject {

    private final String testField;
  }
}
