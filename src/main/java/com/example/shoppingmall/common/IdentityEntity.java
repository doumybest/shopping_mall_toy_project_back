package com.example.shoppingmall.common;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@MappedSuperclass
public abstract class IdentityEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="id", precision=19, scale=0)
    private Long id;

    public Long getId() { return id; }
}
