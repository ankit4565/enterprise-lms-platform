package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.LessonProgress;
import com.enterprise.courseservice.entity.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    Optional<LessonProgress> findByEnrolmentIdAndLessonId(UUID enrolmentId, UUID lessonId);

    List<LessonProgress> findByEnrolmentId(UUID enrolmentId);

    long countByEnrolmentIdAndStatus(UUID enrolmentId, ProgressStatus status);
}
