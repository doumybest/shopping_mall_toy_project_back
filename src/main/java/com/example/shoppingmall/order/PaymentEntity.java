package com.example.shoppingmall.order;

import jakarta.persistence.*;
import com.example.shoppingmall.common.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_payments")
public class PaymentEntity extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="order_id", nullable=false)
    private OrderEntity order;

    @Column(name="provider", length=20, nullable=false)
    private String provider;

    @Column(name="kind", length=20, nullable=false)
    private String kind;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="amount", nullable=false, precision=19, scale=0)
    private long amount;

    @Column(name="reference_code", length=100, nullable=false)
    private String referenceCode;

    public PaymentEntity() {}

    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }
}
