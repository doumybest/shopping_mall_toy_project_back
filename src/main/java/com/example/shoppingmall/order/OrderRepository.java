package com.example.shoppingmall.order;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByIdAndMember_Id(long id,long memberId);
    Optional<OrderEntity> findByMember_IdAndRequestKey(long memberId,String requestKey);
    List<OrderEntity> findAllByMember_IdOrderByIdDesc(long memberId);
}
