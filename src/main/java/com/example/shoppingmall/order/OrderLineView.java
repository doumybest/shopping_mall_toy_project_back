package com.example.shoppingmall.order;

public record OrderLineView(long orderId,long productId,String name,long unitPrice,int quantity) {
    public OrderService.Line toLine() { return new OrderService.Line(productId,name,unitPrice,quantity); }
}
