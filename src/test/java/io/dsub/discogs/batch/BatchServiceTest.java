package io.dsub.discogs.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.argument.handler.ArgumentHandler;
import io.dsub.discogs.batch.exception.InvalidArgumentException;
import io.dsub.discogs.batch.util.JdbcConnectionTester;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class BatchServiceTest {

  BatchService batchService;

  @Mock
  ArgumentHandler handler;

  @Mock
  JdbcConnectionTester tester;

  @Mock
  ConfigurableApplicationContext ctx;

  @Captor
  ArgumentCaptor<String[]> argCaptor;
  @Captor
  ArgumentCaptor<Class<?>> classCaptor;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    batchService = spy(new BatchService());
    doReturn(handler).when(batchService).getArgumentHandler();
    doReturn(tester).when(batchService).getJdbcConnectionTester();
    doReturn(ctx).when(batchService).runSpringApplication(any());
  }

  @Test
  void givenArg__WhenRun__ShouldCallHandlerWithSameArg() throws Exception {
    // given
    String[] args = {"hello", "world"};

    // when
    batchService.run(args);

    // then
    assertAll(
        () -> verify(handler, times(1)).resolve(argCaptor.capture()),
        () -> assertThat(argCaptor.getValue()).isEqualTo(args)
    );
  }

  @Test
  void givenArg__WhenRunCalled__ShouldCallTesterWithSameArg() throws Exception {
    // given
    String[] args = {"hello", "world"};
    doReturn(args).when(handler).resolve(args);

    // when
    batchService.run(args);

    // then
    assertAll(
        () -> verify(tester, times(1)).testConnection(argCaptor.capture()),
        () -> assertThat(argCaptor.getValue()).isEqualTo(args)
    );
  }

  @Test
  void givenArg__WhenConnectionFailed__ShouldThrow() {
    // given
    String[] args = {"hello", "world"};
    doReturn(args).when(handler).resolve(args);
    doThrow(new InvalidArgumentException(args[0])).when(tester).testConnection(args);

    // when
    Throwable t = catchThrowable(() -> batchService.run(args));

    // then
    assertThat(t).hasMessage(args[0]);
  }

  @Test
  void givenConnectionTester__WhenCalledTwice__ShouldReturnUniqueInstance() {
    // given
    doCallRealMethod().when(batchService).getJdbcConnectionTester();
    this.tester = batchService.getJdbcConnectionTester();

    // when
    JdbcConnectionTester that = batchService.getJdbcConnectionTester();

    // then
    assertThat(this.tester).isNotEqualTo(that);
  }

  @Test
  void givenGetArgumentHandler__WhenCalledTwice__ShouldReturnUniqueInstance() {
    // given
    doCallRealMethod().when(batchService).getArgumentHandler();
    this.handler = batchService.getArgumentHandler();

    // when
    ArgumentHandler that = batchService.getArgumentHandler();

    // then
    assertThat(this.handler).isNotEqualTo(that);
  }

  @Test
  void givenArg__WhenRunCalled__ShouldCallRunSpringApplicationWithSameArgs() {
    try (MockedStatic<SpringApplication> app = Mockito.mockStatic(SpringApplication.class)) {
      String[] args = {"hello", "world"};
      app.when(() -> SpringApplication.run(BatchApplication.class, args))
          .thenReturn(ctx);
      doReturn(args).when(handler).resolve(args);
      doCallRealMethod().when(batchService).runSpringApplication(args);

      // when
      ConfigurableApplicationContext that = batchService.run(args);

      // then
      verify(batchService, times(1)).runSpringApplication(args);
      assertThat(this.ctx).isEqualTo(that);
      app.verify(() -> SpringApplication.run(BatchApplication.class, args), times(1));
    } catch (Exception e) {
      fail(e);
    }
  }
}