package io.dsub.discogs.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import io.dsub.discogs.batch.exception.FileDeleteException;
import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleFileUtilTest {

  SimpleFileUtil fileUtil;

  @RegisterExtension
  LogSpy logSpy = new LogSpy();

  @BeforeEach
  void prepare() {
    fileUtil = spy(SimpleFileUtil.builder().appDirectory(RandomString.make()).build());
  }

  @AfterEach
  void cleanUp() {
    try {
      Path appDir = fileUtil.getAppDirectory(false);
      if (!Files.exists(appDir)) {
        return;
      }
      if (fileUtil.getAppDirectory().equals(fileUtil.DEFAULT_APP_DIR)) {
        return;
      }
      try (Stream<Path> files = Files.walk(appDir)) {
        List<Boolean> lists =
            files.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .map(File::delete)
                .collect(Collectors.toList());
        assertThat(lists).doesNotContain(false);
      } catch (IOException e) {
        fail(e);
      }
      this.fileUtil = null;
    } catch (FileException e) {
      fail(e);
    } catch (NullPointerException e) {
      return;
    }
  }

  @Test
  void whenClearAll__ShouldClearEntirePath() throws InterruptedException {
    try {
      List<Path> filePaths = new ArrayList<>();
      Path appDir = fileUtil.getAppDirectory(true);
      filePaths.add(appDir);

      for (int i = 0; i < 10; i++) {
        Path p = fileUtil.getFilePath(RandomString.make(), true);
        filePaths.add(p);
      }

      fileUtil.clearAll();

      for (Path filePath : filePaths) {
        assertThat(filePath).doesNotExist();
      }

    } catch (FileException e) {
      fail(e);
    } finally {
      Thread.sleep(500);
    }
  }

  @Test
  void givenAppDirectoryDoesNotExists__WhenClearCalled__ShouldLogMessage() throws FileException {
    // when
    fileUtil.clearAll();

    // then
    assertThat(logSpy.getLogsByExactLevelAsString(Level.DEBUG, true))
        .hasSize(1)
        .contains("application directory does not exists. skip clear...");
  }

  @Test
  void givenFileNameAndFalse__WhenGetFilePath__ShouldNotGenerateGivenFile() {
    try {
      Path p = fileUtil.getFilePath("test-name", false);
      assertThat(p).doesNotExist().hasFileName("test-name");
    } catch (FileException e) {
      fail(e);
    }
  }

  @Test
  void givenPathThrows__WhenDelete__ShouldLogThenThrowAgain() throws FileException {
    try (MockedStatic<Files> mockFiles = Mockito.mockStatic(Files.class)) {
      Path mockPath = mock(Path.class);
      doReturn(mockPath).when(fileUtil).getAppDirectory(false);
      doReturn(mockPath).when(mockPath).toAbsolutePath();
      doReturn("mock path").when(mockPath).toString();
      mockFiles.when(() -> Files.exists(any())).thenReturn(true);
      mockFiles.when(() -> Files.deleteIfExists(mockPath))
          .thenThrow(new IOException("test io exception"));
      mockFiles.when(() -> Files.walk(mockPath)).thenReturn(Stream.of(mockPath));
      Throwable t = catchThrowable(() -> fileUtil.clearAll());
      assertThat(t)
          .isNotNull()
          .hasMessage("failed to delete mock path");
      mockFiles.reset();
    }
  }

  @Test
  void givenFileNameAndTrue__WhenGetFilePath__ShouldGenerateGivenFile() {
    try {
      Path p = fileUtil.getFilePath("test-name", true);
      assertThat(p).exists().hasFileName("test-name");
    } catch (FileException e) {
      fail(e);
    }
  }

  @Test
  void givenFalse__WhenGetAppDirectory__ShouldNotGenerate() {
    try {
      Path appDir = fileUtil.getAppDirectory(false);
      assertThat(appDir).doesNotExist();
    } catch (FileException e) {
      fail(e);
    }
  }

  @Test
  void givenTrue__WhenGetAppDirectory__ShouldGenerate() {
    try {
      Path appDir = fileUtil.getAppDirectory(true);
      assertThat(appDir).exists();
    } catch (FileException e) {
      fail(e);
    }
  }

  @Test
  void whenCopyCalled__ShouldCloseTheGivenInput() throws IOException {
    InputStream inputStream = null;
    String randStr = RandomString.make(1000);
    String fileName = RandomString.make();
    try {
      File file = getTemporaryFile(randStr.getBytes());
      inputStream = Files.newInputStream(file.toPath(), StandardOpenOption.TRUNCATE_EXISTING);
      inputStream = Mockito.spy(inputStream);
      fileUtil.copy(inputStream, fileName);
      verify(inputStream, times(1)).close();
    } catch (FileException e) {
      fail(e);
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  @Test
  void givenInputStreamThrowsException__WhenCopyCalled__ShouldLogThenThrow() throws IOException {
    InputStream inputStream = Mockito.mock(InputStream.class);
    // given
    doThrow(IOException.class).when(inputStream).transferTo(any());

    // when
    Throwable t = catchThrowable(() -> fileUtil.copy(inputStream, "test"));

    // then
    assertThat(t).hasMessage("failed to copy test");
    assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true))
        .hasSize(1)
        .contains("failed to copy test");
  }

  @Test
  void whenCopyCalledTwice__ShouldCopyGivenInput() throws IOException {
    InputStream inputStream = null;
    String fileName = RandomString.make();
    String randStr = RandomString.make(1000);
    StandardOpenOption opt = StandardOpenOption.TRUNCATE_EXISTING;

    try {
      File file = getTemporaryFile(randStr.getBytes());
      inputStream = Files.newInputStream(file.toPath(), opt);
      fileUtil.copy(inputStream, fileName);

      File copied = fileUtil.getFilePath(fileName).toFile();
      assertThat(copied).exists().hasSize(1000).hasContent(randStr);

      Files.delete(copied.toPath());
      randStr += randStr;
      Files.write(file.toPath(), randStr.getBytes(), opt);
      inputStream = Files.newInputStream(file.toPath(), opt);

      fileUtil.copy(inputStream, fileName);
      copied = fileUtil.getFilePath(fileName).toFile();
      assertThat(copied).exists().hasSize(2000).hasContent(randStr);

    } catch (FileException e) {
      fail(e);
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  @Test
  void whenDefaultConstructorCalled__ShouldHaveProperDefaultValues() {
    // when
    this.fileUtil = SimpleFileUtil.builder().build();
    // then
    assertThat(fileUtil.getAppDirectory()).isEqualTo(FileUtil.DEFAULT_APP_DIR);
    assertThat(fileUtil.isTemporary()).isTrue();
  }

  @Test
  void whenBuilderCalled__ShouldBehaveAsExpected() {
    // when
    this.fileUtil = SimpleFileUtil.builder().isTemporary(true).appDirectory("hello").build();
    // then
    assertThat(this.fileUtil.isTemporary()).isTrue();
    assertThat(this.fileUtil.getAppDirectory()).isEqualTo("hello");
  }

  File getTemporaryFile(byte[] bytes) throws IOException {
    TemporaryFolder folder = new TemporaryFolder();
    folder.create();
    File file = folder.newFile();
    if (bytes == null) {
      return file;
    }
    Files.write(file.toPath(), bytes, StandardOpenOption.TRUNCATE_EXISTING);
    return file;
  }

  @Test
  void givenSimpleFileUtilCreated__WhenConstructorCalled__ShouldContainAppDirPath() {
    // when
    this.fileUtil = SimpleFileUtil.builder().appDirectory("test").isTemporary(true).build();

    // then
    assertThat(fileUtil.getAppDirPath())
        .isNotNull()
        .isEqualTo(Path.of(fileUtil.getHomeDirectory().toFile().getAbsolutePath(),
            fileUtil.getAppDirectory()));
  }

  @Test
  void givenPathThrowsIOException__WhenCollectByWalkDir__ShouldLog() {
    try (MockedStatic<Files> mockFiles = Mockito.mockStatic(Files.class)) {
      mockFiles.when(() -> Files.walk(any())).thenThrow(new IOException("test"));
      Throwable t = catchThrowable(() -> this.fileUtil.collectByWalkDir(null));

      assertThat(t)
          .isInstanceOf(FileDeleteException.class)
          .hasMessage("failed traversing subdirectories");
      assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true))
          .hasSize(1)
          .contains(t.getMessage());
    }
  }

  @Test
  void givenFilePathThrowsIOException__WhenTryCreateFile__ShouldLog() {
    try (MockedStatic<Files> mockFiles = Mockito.mockStatic(Files.class)) {

      Path p = mock(Path.class);
      doReturn("test path").when(p).toString();
      mockFiles.when(() -> Files.createFile(any())).thenThrow(new IOException("test"));

      Throwable t = catchThrowable(() -> fileUtil.tryCreateFile(p));
      assertThat(t)
          .isInstanceOf(FileException.class)
          .hasMessage("failed to create file: test path");
      assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true))
          .hasSize(1)
          .contains(t.getMessage());
    }
  }

  @Test
  void whenTryCreateDirectoryThrows__ShouldLogThenThrow() {
    try (MockedStatic<Files> mockFiles = Mockito.mockStatic(Files.class)) {

      Path p = Path.of("test path");
      mockFiles.when(() -> Files.createDirectory(p)).thenThrow(new IOException("test"));

      Throwable t = catchThrowable(() -> fileUtil.tryCreateDirectory(p));

      assertThat(t)
          .isInstanceOf(FileException.class)
          .hasMessage("failed to create directory: " + p);

      assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true))
          .hasSize(1)
          .contains(t.getMessage());
    }
  }

  @Test
  void whenDeleteFile__ShouldDeleteFile() throws IOException, FileException {

    Path appDirectory = fileUtil.getAppDirectory(true);

    // given
    Path filePath = Files.createFile(Path.of(appDirectory.toAbsolutePath().toString(), "test"));
    assertThat(filePath.toFile().exists()).isTrue();

    // when
    fileUtil.deleteFile("test");

    // then
    assertThat(filePath.toFile().exists()).isFalse();
  }

  @Test
  void whenFilesDeleteIfExistsThrowsIOException__ShouldLogThenThrow() throws FileException {
    try (MockedStatic<Files> mockFiles = Mockito.mockStatic(Files.class)) {
      Path appDirectory = fileUtil.getAppDirectory(true);
      mockFiles.when(() -> Files.deleteIfExists(any())).thenThrow(new IOException("test"));

      Throwable t = catchThrowable(() -> fileUtil.deleteFile("test file"));
      assertThat(t)
          .isInstanceOf(FileDeleteException.class)
          .hasMessage("failed to delete file: test file");

      assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true))
          .hasSize(1)
          .contains(t.getMessage());
    }
  }

  @Test
  void whenIsExisting__ShouldReturnProperValue() throws FileException, IOException {
    // when
    Path appDir = fileUtil.getAppDirectory(true);
    Path filePath = Files.createFile(Path.of(appDir.toAbsolutePath().toString(), "test"));
    assertThat(filePath).exists();

    // then
    assertThat(fileUtil.isExisting(filePath.getFileName().toString())).isTrue();
    assertThat(fileUtil.isExisting("not there")).isFalse();
  }

  @Test
  void whenGetSize__ShouldReturnProperSize() throws FileException, IOException {
    String source = "test source content";
    InputStream stream = new ByteArrayInputStream(source.getBytes());
    Path appPath = fileUtil.getAppDirectory(true);

    // given
    long size = Files.copy(stream, Path.of(appPath.toAbsolutePath().toString(), "test"));

    // when
    long result = fileUtil.getSize("test");

    // then
    assertThat(result).isEqualTo(size);
  }

  @Test
  void givenFileDoesNotExists__WhenGetSize__ShouldReturnNegativeValue() throws FileException {
    fileUtil.getAppDirectory(true);

    // when
    long size = fileUtil.getSize("do not exist");

    // then
    assertThat(size).isEqualTo(-1);
  }

  @Test
  void whenFilesSizeThrowsException__ShouldLogThenThrow() throws FileException {
    String name = "fake";
    doReturn(true).when(fileUtil).isExisting(name);
    fileUtil.getAppDirectory(true);

    Throwable t = catchThrowable(() -> fileUtil.getSize(name));

    assertThat(t)
        .isInstanceOf(FileException.class)
        .hasMessage("failed to fetch size from fake");

    assertThat(logSpy.getLogsByExactLevelAsString(Level.ERROR, true))
        .hasSize(1)
        .contains(t.getMessage());
  }
}
