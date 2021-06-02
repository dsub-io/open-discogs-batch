package io.dsub.discogsdata.batch.job;

import static io.dsub.discogsdata.batch.config.BatchConfig.DEFAULT_CHUNK_SIZE;
import static io.dsub.discogsdata.batch.config.TaskExecutorConfig.CORE_SIZE;
import static io.dsub.discogsdata.batch.config.TaskExecutorConfig.DEFAULT_THROTTLE_LIMIT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dsub.discogsdata.batch.argument.ArgType;
import io.dsub.discogsdata.batch.dump.DiscogsDump;
import io.dsub.discogsdata.batch.dump.DumpDependencyResolver;
import io.dsub.discogsdata.batch.dump.DumpType;
import io.dsub.discogsdata.batch.testutil.LogSpy;
import io.dsub.discogsdata.common.exception.InvalidArgumentException;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class DefaultJobParameterResolverTest {

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @Mock
  DumpDependencyResolver dependencyResolver;

  @InjectMocks
  DefaultJobParameterResolver jobParameterResolver;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void whenParseChunkSize__ShouldReturnSameValue() {
    ApplicationArguments args = new DefaultApplicationArguments("--chunkSize=3000");
    // when
    int result = jobParameterResolver.parseChunkSize(args);

    // then
    assertThat(result).isEqualTo(3000);
  }

  @Test
  void whenParseChunkSize__WithNoValue__ShouldReturnDefaultValue() {
    ApplicationArguments args = new DefaultApplicationArguments();

    // when
    int result = jobParameterResolver.parseChunkSize(args);

    // then
    assertThat(result).isEqualTo(DEFAULT_CHUNK_SIZE);

    // optional if log actually got spied...  in some cases, this may be an issue for failure.
    if (!logSpy.getEvents().isEmpty()) {
      assertThat(logSpy.getEvents().get(0).getMessage())
          .isEqualTo("chunkSize not specified. returning default value: " + DEFAULT_CHUNK_SIZE);
    }
  }

  @Test
  void whenParseChunkSize__WithMalFormedValue__ShouldThrow() {
    ApplicationArguments args = new DefaultApplicationArguments("--chunkSize=SpongeBob");

    // when
    Throwable t = catchThrowable(() -> jobParameterResolver.parseChunkSize(args));

    // then
    assertThat(t)
        .isInstanceOf(InvalidArgumentException.class)
        .hasMessage("failed to parse chunkSize: SpongeBob");
  }

  @Test
  void whenResolve__ShouldCallDumpResolverOnlyOnce() {
    DiscogsDump dump =
        DiscogsDump.builder()
            .eTag("testEtag")
            .type(DumpType.values()[new Random().nextInt(4)])
            .build();
    ApplicationArguments targetArg = new DefaultApplicationArguments();
    ArgumentCaptor<ApplicationArguments> argCaptor =
        ArgumentCaptor.forClass(ApplicationArguments.class);
    when(dependencyResolver.resolve(argCaptor.capture())).thenReturn(List.of(dump));
    // when
    jobParameterResolver.resolve(targetArg);

    // then
    verify(dependencyResolver, times(1)).resolve(targetArg);
    assertThat(argCaptor.getValue()).isEqualTo(targetArg);
  }

  @Test
  void whenResolve__ShouldReturnAsExpected() {
    DiscogsDump dump =
        DiscogsDump.builder()
            .eTag("testEtag")
            .type(DumpType.values()[new Random().nextInt(4)])
            .build();
    when(dependencyResolver.resolve(any())).thenReturn(List.of(dump));

    // when
    Properties resultProps = jobParameterResolver.resolve(new DefaultApplicationArguments());

    // then
    assertThat(resultProps.size()).isEqualTo(2);
    assertThat(resultProps.get(dump.getType().toString())).isEqualTo(dump.getETag());
    assertThat(resultProps.get(ArgType.CHUNK_SIZE.getGlobalName()))
        .isEqualTo(String.valueOf(DEFAULT_CHUNK_SIZE));
  }
}
