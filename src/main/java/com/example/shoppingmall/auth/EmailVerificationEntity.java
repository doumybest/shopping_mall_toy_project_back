package com.example.shoppingmall.auth;

import jakarta.persistence.*;
import java.time.Instant;
import com.example.shoppingmall.common.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_email_verifications")
public class EmailVerificationEntity {
    @Id
    @Column(name="email", length=200)
    private String email;

    @Column(name="code_hash", length=100, nullable=true)
    private String codeHash;

    @Column(name="session_hash", length=64, nullable=true)
    private String sessionHash;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="expires_at", nullable=true)
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="verified_at", nullable=true)
    private Instant verifiedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="consumed_at", nullable=true)
    private Instant consumedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="sent_at", nullable=true)
    private Instant sentAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="window_started_at", nullable=false)
    private Instant windowStartedAt;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="send_count", nullable=false, precision=10, scale=0)
    private int sendCount;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="attempt_count", nullable=false, precision=10, scale=0)
    private int attemptCount;

    public EmailVerificationEntity() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public String getSessionHash() { return sessionHash; }
    public void setSessionHash(String sessionHash) { this.sessionHash = sessionHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }

    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant consumedAt) { this.consumedAt = consumedAt; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public Instant getWindowStartedAt() { return windowStartedAt; }
    public void setWindowStartedAt(Instant windowStartedAt) { this.windowStartedAt = windowStartedAt; }

    public int getSendCount() { return sendCount; }
    public void setSendCount(int sendCount) { this.sendCount = sendCount; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
}
