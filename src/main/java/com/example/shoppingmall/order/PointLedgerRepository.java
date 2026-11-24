package com.example.shoppingmall.order;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface PointLedgerRepository extends JpaRepository<PointLedgerEntity, Long> {
    @EntityGraph(attributePaths="order")
    List<PointLedgerEntity> findAllByMember_IdOrderByIdDesc(long memberId);
}
