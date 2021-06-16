package io.dsub.discogs.batch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

class JobLaunchingRunnerTest {

  @Mock
  Job job;
  @Mock
  JobParameters discogsJobParameters;
  @Mock
  JobLauncher jobLauncher;
  @Mock
  ApplicationArguments args;
  @InjectMocks
  JobLaunchingRunner runner;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void GivenJobAndJobParameters__WhenRun__ShouldCallJobLauncherWithJobAndJobParameters() {
    try {
    // when
    runner.run(args);

    // then
    verify(jobLauncher, times(1)).run(job, discogsJobParameters);
    } catch (Exception e) {
      fail(e);
    }
  }
}