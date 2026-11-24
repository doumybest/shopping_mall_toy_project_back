package com.example.shoppingmall.cart;

import jakarta.persistence.*;
import com.example.shoppingmall.common.*;
import com.example.shoppingmall.auth.MemberEntity;
import com.example.shoppingmall.product.ProductEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_cart")
public class CartEntity {
    @EmbeddedId
    private MemberProductKey id;

    @MapsId("memberId")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="member_id")
    private MemberEntity member;

    @MapsId("productId")
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="product_id")
    private ProductEntity product;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="quantity", nullable=false, precision=10, scale=0)
    private int quantity;

    public CartEntity() {}

    public MemberProductKey getId() { return id; }
    public void setId(MemberProductKey id) { this.id = id; }

    public MemberEntity getMember() { return member; }
    public void setMember(MemberEntity member) { this.member = member; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
