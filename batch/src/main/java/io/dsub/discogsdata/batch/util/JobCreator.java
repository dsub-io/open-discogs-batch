package io.dsub.discogsdata.batch.util;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;

public interface JobCreator {
    Job make(JobParameters jobParameters);
}
