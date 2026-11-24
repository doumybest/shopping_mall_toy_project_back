package com.example.shoppingmall.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.shoppingmall.common.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_members")
public class MemberEntity extends BaseEntity {
    @Column(name="email", length=200, nullable=false)
    private String email;

    @Column(name="password_hash", length=100, nullable=false)
    private String passwordHash;

    @Column(name="name", length=50, nullable=false)
    private String name;

    @Column(name="role", length=10, nullable=false)
    private String role;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="points", nullable=false, precision=19, scale=0)
    private long points;

    @Column(name="email_verified_at")
    private LocalDateTime emailVerifiedAt;

    public MemberEntity() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getPoints() { return points; }
    public void setPoints(long points) { this.points = points; }

    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
}
