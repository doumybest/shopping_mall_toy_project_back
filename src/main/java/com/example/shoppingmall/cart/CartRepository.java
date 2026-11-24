package com.example.shoppingmall.cart;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.example.shoppingmall.common.MemberProductKey;

public interface CartRepository extends JpaRepository<CartEntity, MemberProductKey> {
    // Scalar projection keeps inventory entities out of the persistence context before locking.
    @Query("select new com.example.shoppingmall.cart.CartRow(p.id,p.name,p.category,p.price,p.description,p.emoji,p.imageUrl,p.stock,p.active,c.quantity) from CartEntity c join c.product p where c.id.memberId=:memberId order by p.id")
    List<CartRow> findRows(@Param("memberId") long memberId);
    void deleteById_MemberId(long memberId);
}
