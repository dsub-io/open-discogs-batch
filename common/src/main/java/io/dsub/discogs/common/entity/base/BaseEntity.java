package io.dsub.discogs.common.entity.base;

import java.io.Serializable;
import javax.persistence.MappedSuperclass;

/** Base entity class which should be implemented around all entity classes. */
@MappedSuperclass
public abstract class BaseEntity implements Serializable {}
