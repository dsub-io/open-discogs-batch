package io.dsub.discogsdata.batch.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

class DtoImplementationTest {

    List<? extends BaseDTO> dtoList = new DtoRegistry().getAllInstances();

    @Test
    void shouldSupportClone() {
        for (BaseDTO instance : dtoList) {
            assertDoesNotThrow(instance::clone);
        }
    }

    @Test
    void shouldCloneToUniqueInstance() {
        for (BaseDTO instance : dtoList) {
            try {
                BaseDTO copied = (BaseDTO) instance.clone();
                assertThat(copied).isNotEqualTo(instance);
            } catch (CloneNotSupportedException ignored) {
                fail();
            }
        }
    }

    @Test
    void shouldReturnIsRelationalMethodCall() {
        for (BaseDTO instance : dtoList) {
            assertDoesNotThrow(instance::isRelational);
            assertThat(instance.isRelational())
                    .isNotNull();
        }
    }

    @Test
    void shouldReturnIsTimeBaseEntityMethodCall() {
        for (BaseDTO instance : dtoList) {
            assertDoesNotThrow(instance::isTimeBaseEntity);
            assertThat(instance.isTimeBaseEntity())
                    .isNotNull();
        }
    }

    @Test
    void shouldCleanFieldsProperly() {
        for (BaseDTO instance : dtoList) {
            try {
                BaseDTO copy = (BaseDTO) instance.clone();
                setStringFieldsToBlank(copy);
                assertStringFieldsAreEmpty(copy);
                copy.cleanFields();
                assertStringFieldsAreNull(copy);
            } catch (CloneNotSupportedException ignored) {
                fail();
            }
        }
    }

    private void assertStringFieldsAreNull(BaseDTO copy) {
        for (Field declaredField : copy.getClass().getDeclaredFields()) {
            if (declaredField.getType().isAssignableFrom(String.class)) {
                assertThat(getDeclaredFieldValue(copy, declaredField))
                        .isNull();
            }
        }
    }

    private void assertStringFieldsAreEmpty(BaseDTO copy) {
        for (Field declaredField : copy.getClass().getDeclaredFields()) {
            if (declaredField.getType().isAssignableFrom(String.class)) {
                assertThat(getDeclaredFieldValue(copy, declaredField))
                .isBlank();
            }
        }
    }

    private String getDeclaredFieldValue(BaseDTO copy, Field declaredField) {
        try {
            if (declaredField.trySetAccessible()) {
                return (String) declaredField.get(copy);
            }
            return "err";
        } catch (IllegalAccessException ignored) {
            return "err";
        }
    }

    private void setStringFieldsToBlank(BaseDTO copy) {
        for (Field declaredField : copy.getClass().getDeclaredFields()) {
            if (declaredField.getType().isAssignableFrom(String.class)) {
                if (declaredField.trySetAccessible()) {
                    setDeclaredValue(copy, declaredField);
                }
            }
        }
    }

    private void setDeclaredValue(BaseDTO copy, Field declaredField) {
        try {
            declaredField.set(copy, "");
        } catch (IllegalAccessException ignored){}
    }

    @Test
    void getEntityName() {
        for (BaseDTO instance : dtoList) {
            assertThat(instance.getTblName())
                    .isNotNull()
                    .isNotBlank();
        }
    }
}