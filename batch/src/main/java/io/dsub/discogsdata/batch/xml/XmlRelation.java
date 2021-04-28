package io.dsub.discogsdata.batch.xml;

import io.dsub.discogsdata.batch.dto.BaseDTO;

import java.util.Collection;

public interface XmlRelation<T extends BaseDTO> {
    Collection<T> toEntities();
}
