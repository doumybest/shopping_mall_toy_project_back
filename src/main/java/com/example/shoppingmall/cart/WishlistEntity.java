package com.example.shoppingmall.cart;

import jakarta.persistence.*;
import com.example.shoppingmall.common.*;
import com.example.shoppingmall.auth.MemberEntity;
import com.example.shoppingmall.product.ProductEntity;

@Entity
@Table(name="mall_wishlist")
public class WishlistEntity {
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

    public WishlistEntity() {}

    public MemberProductKey getId() { return id; }
    public void setId(MemberProductKey id) { this.id = id; }

    public MemberEntity getMember() { return member; }
    public void setMember(MemberEntity member) { this.member = member; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }
}
