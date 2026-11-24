package com.example.shoppingmall.order;

import jakarta.persistence.*;
import com.example.shoppingmall.common.*;
import com.example.shoppingmall.auth.MemberEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_point_ledger")
public class PointLedgerEntity extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="member_id", nullable=false)
    private MemberEntity member;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="order_id", nullable=false)
    private OrderEntity order;

    @Column(name="kind", length=20, nullable=false)
    private String kind;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="amount", nullable=false, precision=19, scale=0)
    private long amount;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="balance_after", nullable=false, precision=19, scale=0)
    private long balanceAfter;

    public PointLedgerEntity() {}

    public MemberEntity getMember() { return member; }
    public void setMember(MemberEntity member) { this.member = member; }

    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }

    public long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(long balanceAfter) { this.balanceAfter = balanceAfter; }
}
