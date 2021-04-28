package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.dump.DumpItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
public class FileCleanupStep implements RestartableStep {

    private final DumpItem dump;

    @Override
    public String getName() {
        return "fileCleanupStep " + dump.getUri().split("/")[2];
    }

    @Override
    public void execute(StepExecution stepExecution) throws JobInterruptedException {
        Path p = Path.of(dump.getUri().split("/")[2]);
        if (Files.exists(p)) {
            int retry = 0;
            while (retry < 10) {
                try{
                    Files.deleteIfExists(p);
                    stepExecution.setExitStatus(ExitStatus.COMPLETED);
                    break;
                } catch(FileSystemException e){
                    retry++;
                    wait(1000);
                    if (retry > 9) {
                        log.error("failed to delete {} due to following reason: {}", p.getFileName(), e.getMessage());
                        stepExecution.setExitStatus(ExitStatus.FAILED);
                    }
                } catch (IOException e) {
                    throw new JobInterruptedException(e.getMessage());
                }
            }
        }
    }

    private void wait(int ms) throws JobInterruptedException {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e){
            throw new JobInterruptedException(e.getMessage());
        }
    }
}
