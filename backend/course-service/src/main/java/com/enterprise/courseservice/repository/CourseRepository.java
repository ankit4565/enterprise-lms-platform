package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.Course;
import com.enterprise.courseservice.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    Optional<Course> findBySlugAndDeletedAtIsNull(String slug);

    Optional<Course> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsBySlug(String slug);

    Page<Course> findByInstructorIdAndDeletedAtIsNull(UUID instructorId, Pageable pageable);

    Page<Course> findByStatusAndDeletedAtIsNull(CourseStatus status, Pageable pageable);
}
