package io.dsub.discogsdata.batch.dto;

public interface DtoQueryBuilder {
    <T extends BaseDTO> String buildInsertQuery(Class<T> clazz, boolean updateIfExists);
    <T extends BaseDTO> String buildCreateCloneTableQuery(Class<T> clazz);
    <T extends BaseDTO> String buildDropCloneTableQuery(Class<T> clazz);
    <T extends BaseDTO> String buildInjectCloneTableQuery(Class<T> clazz);
    <T extends BaseDTO> String buildComparePruneCloneTableQuery(Class<T> clazz);
}
