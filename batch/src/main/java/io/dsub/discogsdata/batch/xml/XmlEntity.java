package io.dsub.discogsdata.batch.xml;

import io.dsub.discogsdata.batch.dto.BaseDTO;

public interface XmlEntity<T extends BaseDTO> {
    T toEntity();
}