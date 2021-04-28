package io.dsub.discogsdata.batch.dto;

import java.lang.reflect.Field;

/**
 * Base class for DTOs that may or may not be an independent entity (or a relational entity)
 *
 * IMPORTANT: getKeyValue() method should be implemented only if the entity is independent.
 */
public abstract class BaseDTO implements Cloneable {

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public abstract String getTblName();

    // this means we do not handle the id, but only rdbms would!
    public abstract boolean isRelational();

    public abstract boolean isTimeBaseEntity();

    // should clean fields (i.e. String field to be null instead of remain blank)
    @SuppressWarnings("unchecked")
    public <T extends BaseDTO> T cleanFields() {
        for (Field declaredField : this.getClass().getDeclaredFields()) {
            // not a type of String
            if (!declaredField.getType().isAssignableFrom(String.class)) {
                continue;
            }

            // if only accessible
            if (declaredField.trySetAccessible()) {
                try {
                    String value = (String) declaredField.get(this);
                    // target check
                    if (value != null && value.isBlank()) {
                        declaredField.set(this, null);
                    }
                } catch (IllegalAccessException ignored) { // we assured this won't be thrown!
                }
            }
        }
        return (T) this;
    }
}
