package io.dsub.discogs.batch.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.dsub.discogs.batch.exception.FileException;
import io.dsub.discogs.batch.testutil.LogSpy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
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
import org.mockito.Mockito;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimpleFileUtilTest {

  SimpleFileUtil fileUtil;

  @RegisterExtension LogSpy logSpy = new LogSpy();

  @BeforeEach
  void prepare() {
    fileUtil = SimpleFileUtil.builder().appDirectory(RandomString.make()).build();
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
            files
                .sorted(Comparator.reverseOrder())
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
  void givenFileNameAndFalse__WhenGetFilePath__ShouldNotGenerateGivenFile() {
    try {
      Path p = fileUtil.getFilePath("test-name", false);
      assertThat(p).doesNotExist().hasFileName("test-name");
    } catch (FileException e) {
      fail(e);
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
    } catch (IOException e) {
      fail(e);
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
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
      assertThat(copied)
          .exists()
          .hasSize(1000)
          .hasContent(randStr);

      Files.delete(copied.toPath());
      randStr += randStr;
      Files.write(file.toPath(), randStr.getBytes(), opt);
      inputStream = Files.newInputStream(file.toPath(), opt);

      fileUtil.copy(inputStream, fileName);
      copied = fileUtil.getFilePath(fileName).toFile();
      assertThat(copied)
          .exists()
          .hasSize(2000)
          .hasContent(randStr);

    } catch (IOException e) {
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
}
