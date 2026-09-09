package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    List<Lesson> findByModuleIdOrderByPositionAsc(UUID moduleId);

    List<Lesson> findByCourseId(UUID courseId);

    Optional<Lesson> findByIdAndModuleId(UUID id, UUID moduleId);

    @Query("SELECT COALESCE(MAX(l.position), 0) FROM Lesson l WHERE l.module.id = :moduleId")
    Integer findMaxPositionByModuleId(@Param("moduleId") UUID moduleId);

    @Query("SELECT COALESCE(SUM(l.durationSeconds), 0) FROM Lesson l WHERE l.courseId = :courseId")
    Integer sumDurationByCourseId(@Param("courseId") UUID courseId);

    long countByCourseId(UUID courseId);

    long countByCourseIdAndIsPublishedTrue(UUID courseId);
}
