package io.dsub.discogs.batch;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.argument.handler.ArgumentHandler;
import io.dsub.discogs.batch.argument.validator.ValidationResult;
import io.dsub.discogs.batch.service.DatabaseValidatorService;
import io.dsub.discogs.batch.service.DefaultDatabaseValidatorService;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BatchServiceTest {

    BatchService batchService;

    @Mock
    ArgumentHandler argumentHandler;

    @Mock
    DefaultDatabaseValidatorService databaseValidatorService;

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
        doReturn(databaseValidatorService).when(batchService).getDatabaseValidatorService();
        doReturn(ctx).when(batchService).runSpringApplication(any());
        doReturn(validationResult).when(databaseValidatorService).validate(any());
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
    void givenArg__WhenRunCalled__ShouldCallTesterWithSameArg() throws Exception {
        // given
        String[] args = {"hello", "world"};
        doReturn(args).when(argumentHandler).resolve(args);
        ArgumentCaptor<ApplicationArguments> captor = ArgumentCaptor.forClass(ApplicationArguments.class);

        // when
        batchService.run(args);

        // then
        assertAll(
                () -> verify(databaseValidatorService, times(1)).validate(captor.capture()),
                () -> assertThat(captor.getValue()).isNotNull(),
                () -> assertThat(captor.getValue().getNonOptionArgs()).contains("hello", "world")
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

    @Test
    void whenGetDatabaseService__ShouldReturnNonNullValue() {
        // given
        doCallRealMethod().when(batchService).getDatabaseValidatorService();

        // when
        DatabaseValidatorService service = batchService.getDatabaseValidatorService();

        // then
        assertThat(service)
                .isNotNull()
                .isNotEqualTo(batchService.getDatabaseValidatorService());
    }

    @Test
    void whenGetDatabaseValidationHasErrors__ShouldPrint() {
        String[] args = new String[]{"hello"};
        validationResult = mock(ValidationResult.class);
        doReturn(false).when(validationResult).isValid();
        doReturn(List.of("A", "B", "C")).when(validationResult).getIssues();
        doReturn(validationResult).when(databaseValidatorService).validate(any());
        doReturn(args).when(argumentHandler).resolve(any());

        // when
        assertDoesNotThrow(() -> batchService.run(args));

        // then
        assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true))
                .isNotEmpty()
                .hasSize(3)
                .contains("A", "B", "C");
    }
}