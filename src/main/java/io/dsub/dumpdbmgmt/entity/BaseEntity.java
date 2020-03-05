package io.dsub.dumpdbmgmt.entity;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.time.LocalDate;

@Getter
public abstract class BaseEntity {

    @Temporal(TemporalType.TIME)
    @DateTimeFormat(style = "M-")
    @CreatedDate
    protected LocalDate createdAt;

    @Temporal(TemporalType.TIME)
    @DateTimeFormat(style = "M-")
    @LastModifiedDate
    protected LocalDate updatedAt;
}
