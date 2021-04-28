package io.dsub.discogsdata.batch.step;

import org.springframework.batch.core.Step;

public interface RestartableStep extends Step {
    @Override
    default boolean isAllowStartIfComplete() {
        return true;
    }

    @Override
    default int getStartLimit() {
        return Integer.MAX_VALUE;
    }
}