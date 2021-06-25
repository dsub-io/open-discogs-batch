package io.dsub.discogs.batch.job.reader;

import io.dsub.discogs.batch.TestArguments;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.xml.StaxEventItemReader;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ProgressBarStaxEventItemReaderTest {

    private final PrintStream stdout = System.out;
    private ByteArrayOutputStream outCaptor;

    @BeforeEach
    void setUp() {
        outCaptor = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outCaptor));
    }

    @ParameterizedTest
    @MethodSource("io.dsub.discogs.batch.TestArguments#itemReaderTestArguments")
    void whenRead__ShouldReturnValidItem(TestArguments.ItemReaderTestArgument arg) throws Exception {

        ProgressBarStaxEventItemReader<?> reader = null;

        int readCount = 0;

        try {
            reader = new ProgressBarStaxEventItemReader<>(arg.getMappedClass(), arg.getXmlPath(), arg.getRootElementName());
            reader.open(new ExecutionContext());
            Object item = reader.read();
            while (item != null) {
                item = reader.read();
                readCount++;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        assertThat(readCount).isGreaterThan(0);
    }

    @ParameterizedTest
    @MethodSource("io.dsub.discogs.batch.TestArguments#itemReaderTestArguments")
    void whenRead__ShouldPrintUpdateToSysErr(TestArguments.ItemReaderTestArgument arg) throws Exception {
        ProgressBarStaxEventItemReader<?> reader = null;

        try {
            reader = new ProgressBarStaxEventItemReader<>(arg.getMappedClass(), arg.getXmlPath(), arg.getRootElementName());
            reader.open(new ExecutionContext());
            Object item = reader.read();
            while (item != null) {
                item = reader.read();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            assertThat(outCaptor.toString()).contains("READ " + arg.getMappedClass().getSimpleName());
        }
    }

    @Test
    void whenFilePathIsNull__ShouldThrow() {
        // when
        Throwable t =
                catchThrowable(() -> new ProgressBarStaxEventItemReader<>(ArtistSubItemsCommand.class, null, ""));

        // then
        assertThat(t)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("filePath cannot be null");
    }

    @Test
    void whenFragmentRootElementsIsNull__ShouldThrow() {
        // when
        Throwable t =
                catchThrowable(() -> new ProgressBarStaxEventItemReader<>(ArtistSubItemsCommand.class, Path.of(TestArguments.BASE_XML_PATH, "artist.xml.gz"), ""));

        // then
        assertThat(t)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least 1 fragmentRootElement is required");
    }

    @Test
    @SuppressWarnings("unchecked")
    void whenOpenCalled__ShouldDelegateOpenCall() throws Exception {

        Class<ArtistSubItemsCommand> mappedClass = ArtistSubItemsCommand.class;
        Path path = Path.of(TestArguments.BASE_XML_PATH, "artist.xml.gz");

        ProgressBarStaxEventItemReader<ArtistSubItemsCommand> reader =
                new ProgressBarStaxEventItemReader<>(mappedClass, path, "artist");

        ExecutionContext ctx = new ExecutionContext();

        Field delegateField = reader.getClass().getDeclaredField("nestedReader");
        delegateField.setAccessible(true);

        StaxEventItemReader<ArtistSubItemsCommand> delegate =
                (StaxEventItemReader<ArtistSubItemsCommand>) delegateField.get(reader);

        delegate = Mockito.spy(delegate);
        delegateField.set(reader, delegate);


        try {
            // when
            reader.open(ctx);

            // then
            verify(delegate, times(1)).open(ctx);

            assertThrows(ItemStreamException.class, () -> reader.open(ctx));

            verify(delegate, times(2)).open(ctx);
        } finally {
            reader.close();
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    void whenAfterPropertiesSet__ShouldCallDelegate() throws Exception {
        ProgressBarStaxEventItemReader<ArtistSubItemsCommand> reader = null;
        try {
            Class<ArtistSubItemsCommand> mappedClass = ArtistSubItemsCommand.class;
            Path path = Path.of(TestArguments.BASE_XML_PATH, "artist.xml.gz");

            reader =
                    new ProgressBarStaxEventItemReader<>(mappedClass, path, "artist");

            Field delegateField = reader.getClass().getDeclaredField("nestedReader");
            delegateField.setAccessible(true);

            StaxEventItemReader<ArtistSubItemsCommand> delegate =
                    (StaxEventItemReader<ArtistSubItemsCommand>) delegateField.get(reader);

            delegate = Mockito.spy(delegate);
            delegateField.set(reader, delegate);

            // when
            reader.afterPropertiesSet();

            // then
            verify(delegate, times(1)).afterPropertiesSet();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
