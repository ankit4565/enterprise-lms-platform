package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.Enrolment;
import com.enterprise.courseservice.entity.EnrolmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnrolmentRepository extends JpaRepository<Enrolment, UUID> {

    Optional<Enrolment> findByCourseIdAndStudentId(UUID courseId, UUID studentId);

    boolean existsByCourseIdAndStudentId(UUID courseId, UUID studentId);

    Page<Enrolment> findByStudentId(UUID studentId, Pageable pageable);

    Page<Enrolment> findByStudentIdAndStatus(UUID studentId, EnrolmentStatus status, Pageable pageable);

    long countByCourseId(UUID courseId);
}
