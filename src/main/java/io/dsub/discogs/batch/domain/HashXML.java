package io.dsub.discogs.batch.domain;

import org.jooq.UpdatableRecord;

/**
 * A contract to enforce containing getHashValue() method on top of getRecord(int parentId).
 *
 * @param <T> a subclass of {@link UpdatableRecord<T>} that this class will produce as an instance.
 */
public interface HashXML<T extends UpdatableRecord<T>> extends SubItemXML<T> {
    int getHashValue();

    /**
     * make hash values from given Strings.
     * if values are null or empty, this will simply return the hashcode from the instance.
     * the same applies to the each value from the arguments.
     *
     * @param values to be hashed
     * @return object's hash if values are empty or null, else return hash from combined strings.
     */
    default int makeHash(String[] values) {
        if (values == null || values.length == 0) {
            return hashCode();
        }
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                sb.append(v);
            }
        }
        return sb.isEmpty() ? this.hashCode() : sb.toString().hashCode();
    }
}