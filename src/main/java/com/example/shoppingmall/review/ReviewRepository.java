package com.example.shoppingmall.review;

import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    @EntityGraph(attributePaths="member")
    List<ReviewEntity> findAllByProduct_IdOrderByIdDesc(long productId);
    Optional<ReviewEntity> findByIdAndMember_Id(long id,long memberId);
}
