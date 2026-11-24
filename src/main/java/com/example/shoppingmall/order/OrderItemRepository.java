package com.example.shoppingmall.order;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    @Query("select new com.example.shoppingmall.order.OrderLineView(i.order.id,i.product.id,i.productName,i.unitPrice,i.quantity) from OrderItemEntity i where i.order.id in :ids order by i.order.id,i.product.id")
    List<OrderLineView> findLines(@Param("ids") Collection<Long> ids);
    boolean existsByOrder_Member_IdAndProduct_IdAndOrder_Status(long memberId,long productId,String status);
}
