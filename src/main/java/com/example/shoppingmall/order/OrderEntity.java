package com.example.shoppingmall.order;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.shoppingmall.common.*;
import com.example.shoppingmall.auth.MemberEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_orders")
public class OrderEntity extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="member_id", nullable=false)
    private MemberEntity member;

    @Column(name="request_key", length=64, nullable=false)
    private String requestKey;

    @Column(name="status", length=20, nullable=false)
    private String status;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="subtotal", nullable=false, precision=19, scale=0)
    private long subtotal;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="points_used", nullable=false, precision=19, scale=0)
    private long pointsUsed;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="amount_paid", nullable=false, precision=19, scale=0)
    private long amountPaid;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="points_earned", nullable=false, precision=19, scale=0)
    private long pointsEarned;

    @Column(name="recipient", length=50, nullable=false)
    private String recipient;

    @Column(name="phone", length=30, nullable=false)
    private String phone;

    @Column(name="address", length=300, nullable=false)
    private String address;

    @Column(name="payment_method", length=20, nullable=false)
    private String paymentMethod;

    @Column(name="cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name="shipping_status", length=20, nullable=false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ShippingStatus shippingStatus = ShippingStatus.PREPARING;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="coupon_event_id", precision=19, scale=0)
    private Long couponEventId;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="coupon_discount", nullable=false, precision=19, scale=0)
    private long couponDiscount;

    public enum ShippingStatus { PREPARING, SHIPPED, DELIVERED }
    public ShippingStatus getShippingStatus() { return shippingStatus; }
    public void setShippingStatus(ShippingStatus shippingStatus) { this.shippingStatus = shippingStatus; }
    public Long getCouponEventId() { return couponEventId; }
    public void setCouponEventId(Long couponEventId) { this.couponEventId = couponEventId; }
    public long getCouponDiscount() { return couponDiscount; }
    public void setCouponDiscount(long couponDiscount) { this.couponDiscount = couponDiscount; }

    public OrderEntity() {}

    public MemberEntity getMember() { return member; }
    public void setMember(MemberEntity member) { this.member = member; }

    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String requestKey) { this.requestKey = requestKey; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getSubtotal() { return subtotal; }
    public void setSubtotal(long subtotal) { this.subtotal = subtotal; }

    public long getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(long pointsUsed) { this.pointsUsed = pointsUsed; }

    public long getAmountPaid() { return amountPaid; }
    public void setAmountPaid(long amountPaid) { this.amountPaid = amountPaid; }

    public long getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(long pointsEarned) { this.pointsEarned = pointsEarned; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
