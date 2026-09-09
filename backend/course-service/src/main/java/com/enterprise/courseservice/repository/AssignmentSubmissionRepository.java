package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.AssignmentSubmission;
import com.enterprise.courseservice.entity.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findTopByAssignmentIdAndStudentIdOrderByAttemptNoDesc(UUID assignmentId, UUID studentId);

    List<AssignmentSubmission> findAllByAssignmentIdAndStudentIdOrderByAttemptNoAsc(UUID assignmentId, UUID studentId);

    Page<AssignmentSubmission> findAllByAssignmentId(UUID assignmentId, Pageable pageable);

    Page<AssignmentSubmission> findAllByAssignmentIdAndStatus(UUID assignmentId, SubmissionStatus status, Pageable pageable);

    Page<AssignmentSubmission> findAllByStudentId(UUID studentId, Pageable pageable);

    boolean existsByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);
}
