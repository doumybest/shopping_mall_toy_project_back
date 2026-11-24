package com.example.shoppingmall.order;

import jakarta.persistence.*;
import com.example.shoppingmall.common.*;
import com.example.shoppingmall.product.ProductEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_order_items")
public class OrderItemEntity extends IdentityEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="order_id", nullable=false)
    private OrderEntity order;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="product_id", nullable=false)
    private ProductEntity product;

    @Column(name="product_name", length=120, nullable=false)
    private String productName;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="unit_price", nullable=false, precision=19, scale=0)
    private long unitPrice;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="quantity", nullable=false, precision=10, scale=0)
    private int quantity;

    public OrderItemEntity() {}

    public OrderEntity getOrder() { return order; }
    public void setOrder(OrderEntity order) { this.order = order; }

    public ProductEntity getProduct() { return product; }
    public void setProduct(ProductEntity product) { this.product = product; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
