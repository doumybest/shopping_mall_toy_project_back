package com.example.shoppingmall.order;

import com.example.shoppingmall.auth.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrderController {
    private final OrderService orders;
    private final MemberService members;
    public OrderController(OrderService orders,MemberService members) { this.orders=orders;this.members=members; }
    private long id(Authentication auth) { return members.current(auth).id(); }
    @GetMapping("/api/orders") List<OrderService.Order> list(Authentication auth) { return orders.list(id(auth)); }
    @GetMapping("/api/orders/{orderId}") OrderService.Order detail(Authentication auth,@PathVariable long orderId) { return orders.detail(id(auth),orderId); }
    @PostMapping("/api/orders/checkout") OrderService.Order checkout(Authentication auth,@Valid @RequestBody OrderService.Checkout body) { return orders.checkout(id(auth),body); }
    @PostMapping("/api/orders/{orderId}/cancel") OrderService.Order cancel(Authentication auth,@PathVariable long orderId) { return orders.cancel(id(auth),orderId); }
    @GetMapping("/api/points") List<OrderService.PointEntry> points(Authentication auth) { return orders.points(id(auth)); }
}
