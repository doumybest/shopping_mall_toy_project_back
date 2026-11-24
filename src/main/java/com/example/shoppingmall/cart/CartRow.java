package com.example.shoppingmall.cart;

import com.example.shoppingmall.product.Product;

public record CartRow(long productId,String name,String category,long price,String description,
                      String emoji,String imageUrl,int stock,int active,int quantity) {
    public CartService.Item toItem() {
        return new CartService.Item(new Product(productId,name,category,price,description,emoji,imageUrl,stock,active),quantity);
    }
}
