package com.example.shoppingmall.cart;

import com.example.shoppingmall.auth.MemberService;
import com.example.shoppingmall.common.MemberProductKey;
import com.example.shoppingmall.product.*;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class CartService {
    public record Change(@Min(1) long productId,@Min(1) @Max(99) int quantity) {}
    public record Item(Product product,int quantity) {}
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final MemberService members;
    private final ProductService products;

    public CartService(CartRepository cartRepository,WishlistRepository wishlistRepository,MemberService members,ProductService products) {
        this.cartRepository=cartRepository;
        this.wishlistRepository=wishlistRepository;
        this.members=members;
        this.products=products;
    }
    public List<Item> list(long memberId) { return cartRepository.findRows(memberId).stream().map(CartRow::toItem).toList(); }
    @Transactional
    public void put(long memberId,Change body) {
        var member=members.lockEntity(memberId);
        var product=products.lockEntity(body.productId());
        if(product.getActive()!=1 || product.getStock()<body.quantity())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"販売状況と在庫をご確認ください。");
        var key=new MemberProductKey(memberId,body.productId());
        var item=cartRepository.findById(key).orElseGet(() -> {
            CartEntity created=new CartEntity();
            created.setId(key);created.setMember(member);created.setProduct(product);
            return created;
        });
        item.setQuantity(body.quantity());
        cartRepository.save(item);
    }
    @Transactional
    public void remove(long memberId,long productId) {
        members.lockEntity(memberId);
        cartRepository.findById(new MemberProductKey(memberId,productId)).ifPresent(cartRepository::delete);
    }
    public List<Product> wishlist(long memberId) { return wishlistRepository.findProducts(memberId); }
    @Transactional
    public void wish(long memberId,long productId) {
        var member=members.lockEntity(memberId);
        var product=products.lockEntity(productId);
        if(product.getActive()!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"商品が見つかりません。");
        var key=new MemberProductKey(memberId,productId);
        if(!wishlistRepository.existsById(key)) {
            WishlistEntity wish=new WishlistEntity();
            wish.setId(key);wish.setMember(member);wish.setProduct(product);
            wishlistRepository.save(wish);
        }
    }
    @Transactional
    public void unwish(long memberId,long productId) {
        members.lockEntity(memberId);
        wishlistRepository.findById(new MemberProductKey(memberId,productId)).ifPresent(wishlistRepository::delete);
    }
    @Transactional
    public void clear(long memberId) { cartRepository.deleteById_MemberId(memberId); }
}
