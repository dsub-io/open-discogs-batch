package io.dsub.discogsdata.batch.datasource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DatasourceProxyBeanPostProcessorTest {

  DatasourceProxyBeanPostProcessor proxyBeanPostProcessor;

  @BeforeEach
  void setUp() {
    proxyBeanPostProcessor = spy(new DatasourceProxyBeanPostProcessor());
  }

  @Test
  void whenPostProcessBeforeInitialization__ShouldDoNothingToObject() {

    // given
    Object source = spy(new Object());

    // when
    Object result = proxyBeanPostProcessor.postProcessBeforeInitialization(source, "");

    // then
    assertThat(source, is(result));
    verifyNoInteractions(source);
  }

  @Test
  void givenArgIsNotDataSource__WhenPostProcessAfterInitialization__ShouldNotInteract() {
    // given
    Object source = spy(new Object());

    // when
    Object result = proxyBeanPostProcessor.postProcessAfterInitialization(source, "");

    // then
    assertThat(source, is(result));
    verifyNoInteractions(source);
  }
}
