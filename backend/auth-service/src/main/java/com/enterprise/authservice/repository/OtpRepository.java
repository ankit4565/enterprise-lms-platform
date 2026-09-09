package com.enterprise.authservice.repository;

import com.enterprise.authservice.entity.Otp;
import com.enterprise.authservice.entity.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {

    @Query("SELECT o FROM Otp o WHERE LOWER(o.email) = LOWER(:email) AND o.purpose = :purpose AND o.consumedAt IS NULL ORDER BY o.createdAt DESC LIMIT 1")
    Optional<Otp> findLatestActiveOtp(@Param("email") String email, @Param("purpose") OtpPurpose purpose);
}
