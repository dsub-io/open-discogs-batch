package io.dsub.discogs.batch.util;

import io.dsub.discogs.common.exception.FileFetchException;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link FileUtil} that utilizes NIO package. There is no guarantee that this
 * implementation will provide non-blocking features.
 * <p>
 * This implementation is NOT thread-safe.
 */
@Slf4j
@Getter
public class SimpleFileUtil implements FileUtil {

  private final String appDirectory;
  private final boolean isTemporary;
  private boolean dirCreated = false;
  private final Path appDirPath;

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
   * @throws IOException if any method from {@link Files} has thrown.
   */
  @Override
  public void clearAll() throws IOException {
    Path appDir = getAppDirectory(false);
    if (!Files.exists(appDir)) { // if directory does not exist
      log.debug("application directory does not exists. skip clear...");
      return; // nothing to do, so return.
    }

    List<Path> paths = new ArrayList<>();

    try (Stream<Path> pathStream = Files.walk(appDir)) {
      pathStream.sorted(Comparator.reverseOrder())
          .forEachOrdered(paths::add);
    }

    log.debug("collected {} paths to be deleted.", paths.size());

    for (Path fileOrDir : paths) {
      Files.deleteIfExists(fileOrDir);
      log.debug("deleted {}", fileOrDir);
    }

    log.debug("application directory is cleared.");
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
   * @throws IOException thrown by either {{@link #getAppDirectory(boolean)}} or {@link
   *                     Files#createFile(Path, FileAttribute[])}.
   */
  @Override
  public Path getFilePath(String fileName, boolean generate) throws IOException {
    Path appPath = getAppDirectory(true);
    Path filePath = Path.of(appPath.toString(), fileName);
    if (Files.exists(filePath)) {
      log.debug("grabbing existing file path from {}", filePath.toAbsolutePath());
    }
    if (!Files.exists(filePath) && generate) {
      Files.createFile(filePath);
      log.debug("generated new file at {}.", filePath.toAbsolutePath());
      if (isTemporary) {
        log.debug("marking delete on exit on {}", filePath.toAbsolutePath());
        filePath.toFile().deleteOnExit();
      }
    }
    return filePath;
  }

  /**
   * Generates file path for given file name to the application directory. If application directory
   * does not exists, it will automatically generate one. If you wish to create file at the time
   * getting the path, use {@link #getFilePath(String, boolean)} instead.
   *
   * @param filename required name of the file.
   * @return Generated file path.
   * @throws IOException thrown by either {{@link #getAppDirectory(boolean)}} or {@link
   *                     Files#createFile(Path, FileAttribute[])}.
   */
  @Override
  public Path getFilePath(String filename) throws IOException {
    return getFilePath(filename, false);
  }

  /**
   * Get DIRECTORY where this batch will use. The generation of the directory is idempotent.
   *
   * @param generate if directory should be generated or not.
   * @return application directory.
   * @throws IOException from {@link Files#createDirectory(Path, FileAttribute[])}.
   */
  @Override
  public Path getAppDirectory(boolean generate) throws IOException {
    if (!dirCreated && Files.exists(appDirPath)) { // check if exists
      log.debug("found application directory exists: {}", appDirPath.toAbsolutePath());
      dirCreated = true;
      return appDirPath; // so return as-is.
    }

    if (!dirCreated && generate) {
      log.debug("generating new application directory in {}", appDirPath.toAbsolutePath());
      Files.createDirectory(appDirPath);
      if (isTemporary()) {
        appDirPath.toFile().deleteOnExit();
        log.debug("marked deletion on exit for application directory {}", appDirPath.toAbsolutePath());
      }
      dirCreated = true;
    }
    return appDirPath;
  }

  /**
   * Deletes a file from given path if exists.
   *
   * @param filename a file to be deleted.
   */
  public void deleteFile(String filename) {
    try {
      Files.deleteIfExists(Path.of(appDirPath.toAbsolutePath().toString(), filename));
    } catch (Exception e) {
      throw new FileFetchException(
          "failed to delete file: " + filename + ". reason: " + e.getMessage());
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
  public long getSize(String filename) throws IOException {
    if (isExisting(filename)) {
      return Files.size(Path.of(appDirPath.toAbsolutePath().toString(), filename));
    }
    return -1;
  }

  /**
   * Wrapper method for copying file from input stream. The new copy will replace the pre-existing
   * file.
   *
   * @param inputStream to read from.
   * @param filename    to be written.
   * @throws IOException
   */
  @Override
  public void copy(InputStream inputStream, String filename) throws IOException {
    try (inputStream; inputStream) {
      Path filepath = getFilePath(filename, true);
      Files.copy(inputStream, filepath, StandardCopyOption.REPLACE_EXISTING);
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
        isTemporary = false;
      }
      return new SimpleFileUtil(appDirectory, isTemporary);
    }
  }
}
