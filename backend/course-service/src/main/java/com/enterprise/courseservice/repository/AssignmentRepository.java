package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    Optional<Assignment> findByLessonId(UUID lessonId);
    List<Assignment> findAllByCourseId(UUID courseId);
    boolean existsByLessonId(UUID lessonId);
}
