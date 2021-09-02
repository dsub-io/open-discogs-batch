package io.dsub.discogs.batch.job.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;

class ItemCountingItemProcessListenerTest {

  private ItemCountingItemProcessListener listener;
  private AtomicLong counter;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void setUp() {
    this.counter = Mockito.spy(new AtomicLong(0));
    this.listener = Mockito.spy(new ItemCountingItemProcessListener(counter));
  }

  @Test
  void givenItemIsList__ShouldCountProperly() {
    List<Integer> items = List.of(1, 2, 3, 4, 5);
    listener.afterProcess(items, items);
    assertThat(counter.get()).isEqualTo(5);
  }

  @Test
  void whenCount__ShouldOnlyTouchResult() {
    List<?> item = Mockito.mock(List.class);
    List<?> result = Mockito.mock(List.class);
    given(result.size()).willReturn(5);

    listener.afterProcess(item, result);

    verify(item, times(0)).size();
    verify(result, times(1)).size();
    assertThat(counter.get()).isEqualTo(result.size());
  }

  @Test
  void whenCount__ShouldCountIfResultIsCollection() {
    Collection<?> item = Mockito.mock(Collection.class);
    given(item.size()).willReturn(101);

    listener.afterProcess(item, item);

    assertThat(counter.get()).isEqualTo(item.size());
  }

  @Test
  void whenCount__ShouldCountIfNotCollection() {
    Object object = Mockito.mock(Object.class);
    listener.afterProcess(object, object);
    assertThat(counter.get()).isEqualTo(1);
  }

  @Test
  void whenCount__ShouldSkipNullResult() {
    listener.afterProcess(null, null);
    assertThat(counter.get()).isZero();
  }

  @Test
  void whenCount__ShouldCountSetResult() {
    Set<?> items = Set.of(1,2,3,4,5);
    listener.afterProcess(items, items);
    assertThat(counter.get()).isEqualTo(5);
  }

  @Test
  void whenBeforeProcess__ShouldNoOp() {
    List<?> list = Mockito.spy(List.of());
    listener.beforeProcess(list);
    verify(list, never()).size();
  }

  @Test
  void whenErrorOccur__ShouldLog() {
    Exception e = new Exception("test error");
    String item = "test item";
    listener.onProcessError(item, e);
    List<String> logs = logSpy.getLogsByLevelAsString(Level.ERROR, true);
    assertThat(logs).hasSize(1);
    String log = logs.get(0);
    assertThat(log).contains("test item", "test error");
  }
}
