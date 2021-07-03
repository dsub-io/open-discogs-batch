package io.dsub.discogs.batch;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.testutil.LogSpy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BatchApplicationTest {

    @Mock
    BatchService batchService;

    @Captor
    ArgumentCaptor<String[]> captor;

    @RegisterExtension
    LogSpy logSpy = new LogSpy();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void givenServiceThrows__WhenMainCalled__ShouldLog() {
        try (MockedStatic<BatchApplication> app = Mockito.mockStatic(BatchApplication.class)) {
            String[] args = {"hello world"};
            app.when(BatchApplication::getBatchService).thenReturn(batchService);
            app.when(() -> BatchApplication.main(args)).thenCallRealMethod();
            doThrow(new Exception(args[0])).when(batchService).run(args);
            BatchApplication.main(args);
            List<String> errLogs = logSpy.getLogsByExactLevelAsString(Level.ERROR, true);

            assertAll(
                    () -> assertThat(BatchApplication.getBatchService()).isEqualTo(batchService),
                    () -> verify(batchService, times(1)).run(captor.capture()),
                    () -> assertThat(captor.getValue()).isEqualTo(args),
                    () -> assertThat(errLogs).hasSize(1).allMatch(s -> s.equals(args[0]))
            );

        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void whenGetBatchService__ShouldAlwaysReturnFreshInstance() {
        // when
        BatchService service = BatchApplication.getBatchService();

        // then
        assertThat(service).isNotEqualTo(BatchApplication.getBatchService());
    }

    @Test
    void givenAnyArg__WhenServiceExecuted__ThenReturnNormally() {
        try (MockedStatic<BatchApplication> app = Mockito.mockStatic(BatchApplication.class)) {
            String[] args = {"hello world"};
            app.when(BatchApplication::getBatchService).thenReturn(batchService);
            app.when(() -> BatchApplication.main(args)).thenCallRealMethod();
            assertDoesNotThrow(() -> BatchApplication.main(args));
            app.reset();
        } catch (Exception e) {
            fail(e);
        }
    }
}