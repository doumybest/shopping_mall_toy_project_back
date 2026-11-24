package com.example.shoppingmall.auth;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerificationEntity v where v.email=:email")
    Optional<EmailVerificationEntity> findLockedByEmail(@Param("email") String email);
}
