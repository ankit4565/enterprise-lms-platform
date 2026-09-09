package com.enterprise.userservice.repository;

import com.enterprise.userservice.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByUserId(UUID userId);

    @Query("SELECT p FROM Profile p WHERE " +
           "(:keyword IS NULL OR LOWER(p.headline) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.bio) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.expertiseTags) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Profile> searchProfiles(@Param("keyword") String keyword, Pageable pageable);
}
