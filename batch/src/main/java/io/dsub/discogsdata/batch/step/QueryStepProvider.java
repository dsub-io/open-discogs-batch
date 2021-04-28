package io.dsub.discogsdata.batch.step;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.tasklet.Tasklet;

public interface QueryStepProvider {
    Step get(String stepName, String... queries);
    Tasklet getQueryingTasklet(String[] queries);
}
