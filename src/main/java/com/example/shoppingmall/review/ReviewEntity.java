package com.example.shoppingmall.review;

import jakarta.persistence.*;
import com.example.shoppingmall.common.*;
import com.example.shoppingmall.auth.MemberEntity;
import com.example.shoppingmall.product.ProductEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_reviews")
public class ReviewEntity extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="member_id", nullable=false)
    private MemberEntity member;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="product_id", nullable=false)
    private ProductEntity product;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="rating", nullable=false, precision=1, scale=0)
    private int rating;

    @Column(name="content", length=1000, nullable=false)
    private String content;

    public ReviewEntity() {}

    public MemberEntity getMember() { return member; }
    public void setMember(MemberEntity member) { this.member = member; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
