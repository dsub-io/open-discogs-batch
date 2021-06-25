package io.dsub.discogs.batch.util;

import io.dsub.discogs.batch.exception.FileDeleteException;
import io.dsub.discogs.batch.exception.FileException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementation of {@link FileUtil} that utilizes NIO package. There is no guarantee that this
 * implementation will provide non-blocking features.
 *
 * <p>This implementation is NOT thread-safe.
 */
@Slf4j
@Getter
public class SimpleFileUtil implements FileUtil {

    private final String appDirectory;
    private final boolean isTemporary;
    private final Path appDirPath;
    private boolean dirCreated = false;

    public SimpleFileUtil(String appDirectory, boolean isTemporary) {
        this.appDirectory = appDirectory;
        this.isTemporary = isTemporary;
        appDirPath = Path.of(getHomeDirectory().toFile().getAbsolutePath(), appDirectory);
    }

    public static AppFileUtilBuilder builder() {
        return new AppFileUtilBuilder();
    }

    /**
     * Clears all files and directories used by this application.
     *
     * @throws FileException if any method from {@link Files} has thrown.
     */
    @Override
    public void clearAll() throws FileException {
        Path appDir = getAppDirectory(false);
        if (!Files.exists(appDir)) { // if directory does not exist
            log.debug("application directory does not exists. skip clear...");
            return; // nothing to do, so return.
        }
        List<Path> paths = collectByWalkDir(appDir);
        log.debug("collected {} paths to be deleted.", paths.size());
        clearPaths(paths);
        log.debug("application directory is cleared.");
    }

    private void clearPaths(List<Path> paths) throws FileDeleteException {
        for (Path fileOrDir : paths) {
            try {
                Files.deleteIfExists(fileOrDir);
            } catch (IOException e) {
                throw new FileDeleteException("failed to delete " + fileOrDir.toAbsolutePath(), e);
            }
            log.debug("deleted {}", fileOrDir);
        }
    }

    private List<Path> collectByWalkDir(Path appDir) throws FileDeleteException {
        List<Path> paths = new ArrayList<>();
        try (Stream<Path> pathStream = Files.walk(appDir)) {
            pathStream.sorted(Comparator.reverseOrder()).forEachOrdered(paths::add);
        } catch (IOException e) {
            throw new FileDeleteException("failed traversing subdirectories", e);
        }
        return paths;
    }

    /**
     * Generates file path for given file name to the application directory. If application directory
     * does not exists, it will automatically generate one. If {@param generate} is true, it will
     * generate new file on the target path. And if given file already exists in application
     * directory, this will simply return the path.
     *
     * @param fileName required name of the file.
     * @param generate whether the file has to be generated.
     * @return Generated file path.
     * @throws FileException thrown by either {{@link #getAppDirectory(boolean)}} or {@link
     *                       Files#createFile(Path, FileAttribute[])}.
     */
    @Override
    public Path getFilePath(String fileName, boolean generate) throws FileException {
        Path appPath = getAppDirectory(true);
        Path filePath = Path.of(appPath.toString(), fileName);
        if (Files.exists(filePath)) {
            log.debug("grabbing existing file path from {}", filePath.toAbsolutePath());
        }
        if (!Files.exists(filePath) && generate) {
            tryCreateFile(filePath);
            log.debug("generated new file at {}.", filePath.toAbsolutePath());
            if (isTemporary) {
                log.debug("marking delete on exit on {}", filePath.toAbsolutePath());
                filePath.toFile().deleteOnExit();
            }
            return filePath;
        }
        return filePath;
    }

    private void tryCreateFile(Path filePath) throws FileException {
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            FileException ex = new FileException("failed to crate file: " + filePath, e);
            log.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Generates file path for given file name to the application directory. If application directory
     * does not exists, it will automatically generate one. If you wish to create file at the time
     * getting the path, use {@link #getFilePath(String, boolean)} instead.
     *
     * @param filename required name of the file.
     * @return Generated file path.
     * @throws FileException thrown by either {{@link #getAppDirectory(boolean)}} or {@link
     *                       Files#createFile(Path, FileAttribute[])}.
     */
    @Override
    public Path getFilePath(String filename) throws FileException {
        return getFilePath(filename, false);
    }

    /**
     * Get directory where this batch will use. The generation of the directory is idempotent.
     *
     * @param generate if directory should be generated or not.
     * @return application directory.
     * @throws FileException from {@link Files#createDirectory(Path, FileAttribute[])}.
     */
    @Override
    public Path getAppDirectory(boolean generate) throws FileException {
        if (!dirCreated && Files.exists(appDirPath)) { // check if exists
            log.debug("found application directory exists: {}", appDirPath.toAbsolutePath());
            dirCreated = true;
            return appDirPath; // so return as-is.
        }

        if (!dirCreated && generate) {
            log.debug("generating new application directory in {}", appDirPath.toAbsolutePath());
            tryCreateDirectory(appDirPath);
            if (isTemporary()) {
                appDirPath.toFile().deleteOnExit();
                log.debug(
                        "marked deletion on exit for application directory {}", appDirPath.toAbsolutePath());
            }
            dirCreated = true;
        }
        return appDirPath;
    }

    private void tryCreateDirectory(Path path) throws FileException {
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            FileException ex = new FileException("failed to create directory: " + path, e);
            log.error("failed to create directory", ex);
            throw ex;
        }
    }

    /**
     * Deletes a file from given path if exists.
     *
     * @param filename a file to be deleted.
     */
    public void deleteFile(String filename) throws FileDeleteException {
        try {
            Files.deleteIfExists(Path.of(appDirPath.toAbsolutePath().toString(), filename));
        } catch (Exception e) {
            FileDeleteException ex = new FileDeleteException("failed to delete file: " + filename, e);
            log.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Check file exists by name under application directory.
     *
     * @param filename a file to be checked.
     */
    @Override
    public boolean isExisting(String filename) {
        return Files.exists(Path.of(appDirPath.toAbsolutePath().toString(), filename));
    }

    /**
     * Get file size of target file named as parameter. If file does not exists, this will return -1,
     * else will return as-is.
     *
     * @param filename a file to be checked.
     */
    @Override
    public long getSize(String filename) throws FileException {
        if (isExisting(filename)) {
            try {
                return Files.size(Path.of(appDirPath.toAbsolutePath().toString(), filename));
            } catch (IOException e) {
                FileException ex = new FileException("failed to fetch size from " + filename, e);
                log.error(ex.getMessage(), ex);
                throw ex;
            }
        }
        return -1;
    }

    /**
     * Wrapper method for copying file from input stream. The new copy will replace the pre-existing
     * file.
     *
     * @param inputStream to read from.
     * @param filename    to be written.
     * @throws FileException delegated from IO operation.
     */
    @Override
    public void copy(InputStream inputStream, String filename) throws FileException {
        try (inputStream) {
            Path filepath = getFilePath(filename, true);
            Files.copy(inputStream, filepath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            FileException ex = new FileException("failed to copy " + filename, e);
            log.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Builder pattern for instantiation.
     */
    public static class AppFileUtilBuilder {

        private String appDirectory;
        private boolean appDirectorySet;
        private boolean isTemporary;
        private boolean isTemporarySet;

        AppFileUtilBuilder() {
        }

        public AppFileUtilBuilder appDirectory(String appDirectory) {
            this.appDirectory = appDirectory;
            this.appDirectorySet = true;
            return this;
        }

        public AppFileUtilBuilder isTemporary(boolean isTemporary) {
            this.isTemporary = isTemporary;
            this.isTemporarySet = true;
            return this;
        }

        public SimpleFileUtil build() {
            String appDirectory = this.appDirectory;
            if (!this.appDirectorySet) {
                appDirectory = DEFAULT_APP_DIR;
            }
            boolean isTemporary = this.isTemporary;
            if (!this.isTemporarySet) {
                isTemporary = true;
            }
            return new SimpleFileUtil(appDirectory, isTemporary);
        }
    }
}
