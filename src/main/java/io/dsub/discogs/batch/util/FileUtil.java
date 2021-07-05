package io.dsub.discogs.batch.util;

import io.dsub.discogs.batch.exception.FileDeleteException;
import io.dsub.discogs.batch.exception.FileException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.lang3.SystemUtils;

/**
 * Interface to support file creation, deletion operations only within application directory.
 * Currently, there is no need to utilize second level, hence flattening the method params by
 * String. There is NO GUARANTEE that this interface will not grow in the future.
 *
 * <p>Methods and their behaviors will depends on its implementations.
 */
public interface FileUtil {

  String DEFAULT_APP_DIR = "discogs-data-batch";

  void clearAll() throws FileException;

  Path getFilePath(String filename, boolean generate) throws FileException;

  Path getFilePath(String filename) throws FileException;

  Path getAppDirectory(boolean generate) throws FileException;

  void deleteFile(String filename) throws FileDeleteException;

  boolean isExisting(String filename);

  long getSize(String filename) throws FileException;

  void copy(InputStream inputStream, String filename) throws FileException;

  /**
   * A wrapper method to get home directory from the parent OS. The implementation of the method may
   * change over time, in case of bugs or requirements of additional logics.
   *
   * @return directory promised to be home directory (i.e. `~`.)
   */
  default Path getHomeDirectory() {
    return SystemUtils.getUserHome().toPath();
  }

  String getAppDirectory();

  boolean isTemporary();
}
