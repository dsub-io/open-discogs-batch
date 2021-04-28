package io.dsub.discogsdata.batch;

import io.dsub.discogsdata.batch.dto.BaseDTO;
import io.dsub.discogsdata.batch.dto.DtoRegistry;
import io.dsub.discogsdata.batch.entity.artist.dto.ArtistDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class DtoRegistryTest {

    private DtoRegistry dtoRegistry;
    private Set<String> dtoClassNames;

    @BeforeEach
    void setUp() {
        dtoRegistry = new DtoRegistry();
        dtoClassNames = getDtoClassNames();
    }

    @Test
    void chkRegistrySizeMatches() {
        dtoClassNames.forEach(className -> {
            log.debug("className: {}", className);
        });
        assertThat(dtoRegistry.getSize())
                .isEqualTo(dtoClassNames.size());
    }

    @Test
    void registeredClassesShouldMatchActualImplementedClasses() {
        Set<String> classNames = dtoRegistry.getAllInstances().parallelStream()
                .map(item -> item.getClass().getSimpleName())
                .collect(Collectors.toSet());

        for (String dtoClassName : dtoClassNames) {
            if (classNames.contains(dtoClassName)) continue;
            System.out.println("MISSING " + dtoClassName);
        }

        for (String className : classNames) {
            if (!dtoClassNames.contains(className)) {
                log.error("MISSING " + className);
            }
            assertTrue(dtoClassNames.contains(className));
        }

        assertThat(dtoClassNames.size()).isEqualTo(classNames.size());
    }

    @Test
    void fetchedInstanceShouldBeUnique() {
        Object a = dtoRegistry.getInstanceOf(ArtistDTO.class);
        Object b = dtoRegistry.getInstanceOf(ArtistDTO.class);
        assertThat(a).isNotEqualTo(b);
    }

    private Set<String> getDtoClassNames() {
        return new Reflections(DtoRegistry.BASE_DTO_PACKAGE)
                .getSubTypesOf(BaseDTO.class)
                .stream()
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers()))
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
    }
}