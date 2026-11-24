package com.example.shoppingmall.common;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Embeddable
public class MemberProductKey implements Serializable {
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="member_id", precision=19, scale=0)
    private Long memberId;
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="product_id", precision=19, scale=0)
    private Long productId;

    public MemberProductKey() {}
    public MemberProductKey(Long memberId, Long productId) { this.memberId=memberId;this.productId=productId; }
    public Long getMemberId() { return memberId; }
    public Long getProductId() { return productId; }
    @Override public boolean equals(Object other) {
        return other instanceof MemberProductKey key && Objects.equals(memberId,key.memberId) && Objects.equals(productId,key.productId);
    }
    @Override public int hashCode() { return Objects.hash(memberId,productId); }
}
