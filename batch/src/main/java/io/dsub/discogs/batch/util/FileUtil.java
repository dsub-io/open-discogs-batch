package io.dsub.discogs.batch.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.apache.commons.lang3.SystemUtils;

/**
 * Interface to support file creation, deletion operations only within application directory.
 * Currently, there is no need to utilize second level, hence flattening the method params by
 * String. There is NO GUARANTEE that this interface will not grow in the future.
 * <p>
 * Methods and their behaviors will depends on its implementations.
 */
public interface FileUtil {

  String DEFAULT_APP_DIR = "discogs-data-batch";

  void clearAll() throws IOException;

  Path getFilePath(String filename, boolean generate) throws IOException;

  Path getFilePath(String filename) throws IOException;

  Path getAppDirectory(boolean generate) throws IOException;

  void deleteFile(String filename) throws IOException;

  boolean isExisting(String filename);

  long getSize(String filename) throws IOException;

  void copy(InputStream inputStream, String filename) throws IOException;

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
