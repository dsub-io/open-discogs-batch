package io.dsub.discogs.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.argument.handler.ArgumentHandler;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class BatchServiceTest {

  BatchService batchService;

  @Mock
  ArgumentHandler argumentHandler;

  @Mock
  ConfigurableApplicationContext ctx;

  @Captor
  ArgumentCaptor<String[]> argCaptor;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  ValidationResult validationResult;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    batchService = spy(new BatchService());
    validationResult = Mockito.mock(ValidationResult.class);
    doReturn(argumentHandler).when(batchService).getArgumentHandler();
    doReturn(ctx).when(batchService).runSpringApplication(any());
    doReturn(true).when(validationResult).isValid();
  }

  @Test
  void givenArg__WhenRun__ShouldCallHandlerWithSameArg() throws Exception {
    // given
    String[] args = {"hello", "world"};
    doReturn(args).when(argumentHandler).resolve(any());

    // when
    batchService.run(args);

    // then
    assertAll(
        () -> verify(argumentHandler, times(1)).resolve(argCaptor.capture()),
        () -> assertThat(argCaptor.getValue()).isEqualTo(args)
    );
  }

  @Test
  void givenArg__WhenRunCalled__ShouldCallHandlerWithSameArg() throws Exception {
    // given
    String[] args = {"hello", "world"};
    doReturn(args).when(argumentHandler).resolve(argCaptor.capture());

    // when
    batchService.run(args);

    // then
    assertAll(
        () -> assertThat(argCaptor.getValue()).isNotNull(),
        () -> assertThat(argCaptor.getValue()).contains("hello", "world")
    );
  }

  @Test
  void givenGetArgumentHandler__WhenCalledTwice__ShouldReturnUniqueInstance() {
    // given
    doCallRealMethod().when(batchService).getArgumentHandler();
    this.argumentHandler = batchService.getArgumentHandler();

    // when
    ArgumentHandler that = batchService.getArgumentHandler();

    // then
    assertThat(this.argumentHandler).isNotEqualTo(that);
  }

  @Test
  void whenRunCalled__ShouldCallRunSpringApplicationWithSameArgs() {
    try (MockedStatic<SpringApplication> app = Mockito.mockStatic(SpringApplication.class)) {
      String[] args = {"hello", "world"};

      app.when(() -> SpringApplication.run(BatchApplication.class, args)).thenReturn(ctx);
      doReturn(args).when(argumentHandler).resolve(args);
      doReturn(true).when(validationResult).isValid();
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