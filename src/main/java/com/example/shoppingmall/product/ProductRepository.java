package com.example.shoppingmall.product;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    List<ProductEntity> findAllByActiveOrderByIdAsc(int active);
    Optional<ProductEntity> findByIdAndActive(long id,int active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductEntity p where p.id=:id")
    Optional<ProductEntity> findLockedById(@Param("id") long id);
}
