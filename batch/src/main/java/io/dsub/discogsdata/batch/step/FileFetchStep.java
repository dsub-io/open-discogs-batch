package io.dsub.discogsdata.batch.step;

import io.dsub.discogsdata.batch.dump.DumpItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.wrapped.ProgressBarWrappedInputStream;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.dsub.discogsdata.batch.config.AppConfig.PROGRESS_BAR_BUILDER;

@Slf4j
@RequiredArgsConstructor
public class FileFetchStep implements RestartableStep {

    private final DumpItem dump;

    @Override
    public String getName() {
        return "fileFetchStep " + dump.getUri().split("/")[2];
    }

    @Override
    public void execute(StepExecution stepExecution) throws JobInterruptedException {
        Path path = Path.of(dump.getUri().split("/")[2]);

        if (Files.exists(path)) {
            log.info("found duplicated file: {}. checking size...", path);
            if (isFileSizeMatch(path, dump.getSize())) {
                log.info("file already exists. proceeding...");
                stepExecution.setExitStatus(ExitStatus.COMPLETED);
                return;
            }
            if (deleteFile(path)) {
                log.info("incomplete size. deleted previous {}.", path);
            } else {
                stepExecution.setExitStatus(ExitStatus.FAILED);
                return;
            }
        }

        if (fetchFile()) {
            log.info("successfully fetched {}.", path);
            stepExecution.setExitStatus(ExitStatus.COMPLETED);
            return;
        }

        log.error("failed to fetch {}", path);
        stepExecution.setExitStatus(ExitStatus.FAILED);
    }

    private boolean deleteFile(Path path) {
        try {
            if (!Files.exists(path)) {
                return true;
            }
            Files.delete(path);
            return true;
        } catch (IOException ignored) {
            log.error("failed to delete file " + path);
        }
        return false;
    }

    private boolean isFileSizeMatch(Path path, long expectedSize) throws JobInterruptedException {
        assert (Files.exists(path));
        boolean isMatch;
        try {
            isMatch = Files.size(path) == expectedSize;
        } catch (IOException e) {
            throw new JobInterruptedException(e.getMessage());
        }
        return isMatch;
    }

    private InputStream wrapInputStream(InputStream in, String taskName) {
        return new ProgressBarWrappedInputStream(in, PROGRESS_BAR_BUILDER
                .setTaskName(taskName)
                .setInitialMax(dump.getSize())
                .build());
    }

    private boolean fetchFile() {
        Path path = Path.of(dump.getUri().split("/")[2]);
        try (InputStream in = wrapInputStream(
                new URL(dump.getResourceUrl()).openStream(), "FETCHING... " + path)) {
            Files.copy(in, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
