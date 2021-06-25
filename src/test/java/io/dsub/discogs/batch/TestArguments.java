package io.dsub.discogs.batch;

import io.dsub.discogs.batch.domain.artist.ArtistCommand;
import io.dsub.discogs.batch.domain.artist.ArtistSubItemsCommand;
import io.dsub.discogs.batch.domain.label.LabelCommand;
import io.dsub.discogs.batch.domain.label.LabelSubItemsCommand;
import io.dsub.discogs.batch.domain.master.MasterCommand;
import io.dsub.discogs.batch.domain.master.MasterSubItemsCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemCommand;
import io.dsub.discogs.batch.domain.release.ReleaseItemSubItemsCommand;
import io.dsub.discogs.common.entity.BaseEntity;
import lombok.Data;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestArguments {
    public static final String BASE_PKG = "io.dsub.discogs";
    public static final String ENTITY_PKG = "io.dsub.discogs.common";
    public static final String BASE_XML_PATH = "src/test/resources/test/reader";
    public static final String ARTIST = "artist";
    public static final String LABEL = "label";
    public static final String MASTER = "master";
    public static final String RELEASE = "release";

    public static final Reflections REFLECTIONS = new Reflections(BASE_PKG);
    public static final List<Class<? extends BaseEntity>> ENTITIES =
            REFLECTIONS.getSubTypesOf(BaseEntity.class).stream()
                    .filter(clazz -> clazz.getPackageName().contains(ENTITY_PKG))
                    .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                    .filter(clazz -> !Modifier.isInterface(clazz.getModifiers()))
                    .collect(Collectors.toList());

    public static Stream<Class<? extends BaseEntity>> entities() {
        return ENTITIES.stream();
    }

    public static Stream<String> coreEntityNames() {
        return List.of(ARTIST, LABEL, MASTER, RELEASE).stream();
    }

    public static Stream<Path> xmlPaths() {
        return coreEntityNames().map(item -> Path.of(BASE_XML_PATH, item + ".xml.gz"));
    }


    @Data
    public static class ItemReaderTestArgument {
        private Class<?> mappedClass;
        private String rootElementName;
        private Path xmlPath;

        public ItemReaderTestArgument(Class<?> mappedClass, String rootElementName) {
            this.mappedClass = mappedClass;
            this.rootElementName = rootElementName;
            this.xmlPath = Path.of(BASE_XML_PATH, rootElementName + ".xml.gz");
        }
    }

    public static Stream<ItemReaderTestArgument> itemReaderTestArguments() {
        return List.of(
                new ItemReaderTestArgument(ArtistCommand.class, "artist"),
                new ItemReaderTestArgument(ArtistSubItemsCommand.class, "artist"),
                new ItemReaderTestArgument(LabelCommand.class, "label"),
                new ItemReaderTestArgument(LabelSubItemsCommand.class, "label"),
                new ItemReaderTestArgument(MasterCommand.class, "master"),
                new ItemReaderTestArgument(MasterSubItemsCommand.class, "master"),
                new ItemReaderTestArgument(ReleaseItemCommand.class, "release"),
                new ItemReaderTestArgument(ReleaseItemSubItemsCommand.class, "release")).stream();
    }
}