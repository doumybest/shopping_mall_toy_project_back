package com.example.shoppingmall.order;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Slice<OrderEntity> findAllByOrderByIdDesc(Pageable pageable);
    @Query("select o.member.id from OrderEntity o where o.id = :id")
    Optional<Long> findMemberId(@Param("id") long id);

    Optional<OrderEntity> findByIdAndMember_Id(long id,long memberId);
    Optional<OrderEntity> findByMember_IdAndRequestKey(long memberId,String requestKey);
    List<OrderEntity> findAllByMember_IdOrderByIdDesc(long memberId);
}
