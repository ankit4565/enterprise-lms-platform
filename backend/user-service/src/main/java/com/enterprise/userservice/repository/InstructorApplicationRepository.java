package com.enterprise.userservice.repository;

import com.enterprise.userservice.entity.ApplicationStatus;
import com.enterprise.userservice.entity.InstructorApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstructorApplicationRepository extends JpaRepository<InstructorApplication, UUID> {

    List<InstructorApplication> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndStatus(UUID userId, ApplicationStatus status);

    Page<InstructorApplication> findByStatus(ApplicationStatus status, Pageable pageable);
}
