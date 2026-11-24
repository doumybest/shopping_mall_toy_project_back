package com.example.shoppingmall.cart;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.example.shoppingmall.common.MemberProductKey;

public interface WishlistRepository extends JpaRepository<WishlistEntity, MemberProductKey> {
    @Query("select new com.example.shoppingmall.product.Product(p.id,p.name,p.category,p.price,p.description,p.emoji,p.imageUrl,p.stock,p.active) from WishlistEntity w join w.product p where w.id.memberId=:memberId and p.active=1 order by p.id")
    List<com.example.shoppingmall.product.Product> findProducts(@Param("memberId") long memberId);
}
