package com.example.shoppingmall.common;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity extends IdentityEntity {
    @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void initializeCreatedAt() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
