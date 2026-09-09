package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {

    List<Module> findByCourseIdOrderByPositionAsc(UUID courseId);

    Optional<Module> findByIdAndCourseId(UUID id, UUID courseId);

    @Query("SELECT COALESCE(MAX(m.position), 0) FROM Module m WHERE m.course.id = :courseId")
    Integer findMaxPositionByCourseId(@Param("courseId") UUID courseId);
}
