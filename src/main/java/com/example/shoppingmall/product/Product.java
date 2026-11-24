package com.example.shoppingmall.product;

/** API response DTO. ProductEntity is the JPA mapping for the database table. */
public record Product(long id, String name, String category, long price, String description,
                      String emoji, String imageUrl, int stock, int active) {}
