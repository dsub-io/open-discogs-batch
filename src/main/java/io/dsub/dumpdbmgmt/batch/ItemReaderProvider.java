package io.dsub.dumpdbmgmt.batch;

import io.dsub.dumpdbmgmt.entity.Release;
import io.dsub.dumpdbmgmt.repository.ReleaseRepository;
import io.dsub.dumpdbmgmt.xmlobj.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MissingRequiredPropertiesException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Sort;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.security.InvalidParameterException;
import java.util.Collections;

/**
 * Component to deliver CustomStaxEvenItemReader.
 */

@Slf4j
@Configuration
@PropertySource("classpath:application.properties")
public class ItemReaderProvider {

    Environment env;

    public ItemReaderProvider(Environment env) {
        this.env = env;
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlMaster> masterReader() {
        return reader(XmlMaster.class);
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlRelease> releaseReader() {
        return reader(XmlRelease.class);
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlLabel> labelReader() {
        return reader(XmlLabel.class);
    }

    @Bean
    @StepScope
    public CustomStaxEventItemReader<XmlArtist> artistReader() {
        return reader(XmlArtist.class);
    }

    /**
     * @param t   entity class
     * @param <T> entity type
     * @return CustomEventItemReader of given type.
     */
    public <T> CustomStaxEventItemReader<T> reader(Class<T> t) {

        String resource;

        if (!t.getSuperclass().equals(XmlObject.class)) {
            throw new InvalidParameterException("Parameter must extend XmlObject.class");
        }

        if (t.equals(XmlRelease.class)) {
            resource = env.getProperty("resource.release");
        } else if (t.equals(XmlArtist.class)) {
            resource = env.getProperty("resource.artist");
        } else if (t.equals(XmlLabel.class)) {
            resource = env.getProperty("resource.label");
        } else if (t.equals(XmlMaster.class)) {
            resource = env.getProperty("resource.master");
        } else {
            throw new InvalidParameterException("Parameter must be one of following list: [XmlArtist, XmlRelease, XmlLabel, XmlMaster]");
        }

        if (resource == null) {
            throw new MissingRequiredPropertiesException();
        }

        boolean duplicate = false;
        int index = -1;
        String[] types = new String[]{"release", "artist", "label", "master"};
        for (int i = 0; i < types.length; i++) {
            if (resource.contains(types[i])) {
                if (index != -1) {
                    duplicate = true;
                }
                index = i;
            }
        }
        if (index == -1) {
            throw new UnsupportedOperationException("File must contain one of following letters: 'artist', 'release', 'label', 'master'.");
        }
        if (duplicate) {
            throw new UnsupportedOperationException("File must contain *only* one of following letters: 'artist', 'release', 'label', 'master'.");
        }

        StaxEventItemReader<T> reader =
                new StaxEventItemReaderBuilder<T>()
                        .name(t.getName())
                        .resource(new FileSystemResource(resource))
                        .addFragmentRootElements(types[index])
                        .strict(true)
                        .saveState(false)
                        .unmarshaller(getMarshaller(t))
                        .build();
        return new CustomStaxEventItemReader<>(reader);
    }

    private <T> Jaxb2Marshaller getMarshaller(Class<T> t) {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(t);
        return marshaller;
    }

    @Bean
    public RepositoryItemReader<Release> releaseRepositoryItemReader(ReleaseRepository releaseRepository) {
        RepositoryItemReaderBuilder<Release> builder = new RepositoryItemReaderBuilder<>();
        return builder.repository(releaseRepository)
                .name("id")
                .methodName("findAll")
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }
}
