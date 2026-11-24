package com.example.shoppingmall.product;

import jakarta.persistence.*;
import com.example.shoppingmall.common.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name="mall_products")
public class ProductEntity extends BaseEntity {
    @Column(name="name", length=120, nullable=false)
    private String name;

    @Column(name="category", length=40, nullable=false)
    private String category;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="price", nullable=false, precision=19, scale=0)
    private long price;

    @Lob
    @Column(name="description", nullable=false)
    private String description;

    @Column(name="emoji", length=20, nullable=false)
    private String emoji;

    @Column(name="image_url", length=1000, nullable=true)
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="stock", nullable=false, precision=10, scale=0)
    private int stock;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name="active", nullable=false, precision=1, scale=0)
    private int active;

    public ProductEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public int getActive() { return active; }
    public void setActive(int active) { this.active = active; }
}
