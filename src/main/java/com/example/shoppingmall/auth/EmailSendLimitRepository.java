package com.example.shoppingmall.auth;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmailSendLimitRepository extends JpaRepository<EmailSendLimitEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from EmailSendLimitEntity l where l.sourceHash=:key")
    Optional<EmailSendLimitEntity> findLockedByKey(@Param("key") String key);
}
