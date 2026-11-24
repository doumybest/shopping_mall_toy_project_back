package com.example.shoppingmall.auth;

import jakarta.persistence.*;
import java.time.Instant;
import com.example.shoppingmall.common.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_email_send_limits")
public class EmailSendLimitEntity {
    @Id
    @Column(name="source_hash", length=64)
    private String sourceHash;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name="window_started_at", nullable=false)
    private Instant windowStartedAt;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="send_count", nullable=false, precision=10, scale=0)
    private int sendCount;

    public EmailSendLimitEntity() {}

    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }

    public Instant getWindowStartedAt() { return windowStartedAt; }
    public void setWindowStartedAt(Instant windowStartedAt) { this.windowStartedAt = windowStartedAt; }

    public int getSendCount() { return sendCount; }
    public void setSendCount(int sendCount) { this.sendCount = sendCount; }
}
