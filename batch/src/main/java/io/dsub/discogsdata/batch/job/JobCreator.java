package io.dsub.discogsdata.batch.job;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;

public interface JobCreator {

  Job createJob(JobParameters jobParameters) throws Exception;
}
