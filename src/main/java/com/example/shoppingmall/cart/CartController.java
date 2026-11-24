package com.example.shoppingmall.cart;

import com.example.shoppingmall.auth.MemberService;
import com.example.shoppingmall.product.Product;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class CartController {
    private final CartService carts;
    private final MemberService members;
    public CartController(CartService carts,MemberService members) { this.carts=carts; this.members=members; }
    private long id(Authentication auth) { return members.current(auth).id(); }
    @GetMapping("/api/cart") List<CartService.Item> cart(Authentication auth) { return carts.list(id(auth)); }
    @PutMapping("/api/cart") void put(Authentication auth,@Valid @RequestBody CartService.Change body) { carts.put(id(auth),body); }
    @DeleteMapping("/api/cart/{productId}") void remove(Authentication auth,@PathVariable long productId) { carts.remove(id(auth),productId); }
    @GetMapping("/api/wishlist") List<Product> wishlist(Authentication auth) { return carts.wishlist(id(auth)); }
    @PutMapping("/api/wishlist/{productId}") void wish(Authentication auth,@PathVariable long productId) { carts.wish(id(auth),productId); }
    @DeleteMapping("/api/wishlist/{productId}") void unwish(Authentication auth,@PathVariable long productId) { carts.unwish(id(auth),productId); }
}
