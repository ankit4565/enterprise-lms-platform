package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.courseservice.dto.request.*;
import com.enterprise.courseservice.dto.response.LessonResponse;
import com.enterprise.courseservice.dto.response.ModuleResponse;
import com.enterprise.courseservice.entity.Course;
import com.enterprise.courseservice.entity.Lesson;
import com.enterprise.courseservice.entity.Module;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.LessonRepository;
import com.enterprise.courseservice.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleLessonService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    private Course verifyCourseOwnership(UUID courseId, UUID requesterId, List<String> requesterRoles) {
        Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        boolean isAdmin = requesterRoles != null && (requesterRoles.contains("ADMIN") || requesterRoles.contains("SUPER_ADMIN"));
        if (!isAdmin && (requesterId == null || !requesterId.equals(course.getInstructorId()))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "You are not authorized to modify this course");
        }
        return course;
    }

    @Transactional
    public ModuleResponse addModule(UUID courseId, UUID requesterId, List<String> requesterRoles, CreateModuleRequest request) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);

        int nextPosition = moduleRepository.findMaxPositionByCourseId(courseId) + 1;

        Module module = Module.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .position(nextPosition)
                .build();

        Module saved = moduleRepository.save(module);
        log.info("Added module {} to course {}", saved.getId(), courseId);
        return ModuleResponse.fromEntity(saved);
    }

    @Transactional
    public ModuleResponse updateModule(UUID courseId, UUID moduleId, UUID requesterId, List<String> requesterRoles, UpdateModuleRequest request) {
        verifyCourseOwnership(courseId, requesterId, requesterRoles);

        Module module = moduleRepository.findByIdAndCourseId(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));

        if (request.getTitle() != null) {
            module.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            module.setDescription(request.getDescription());
        }

        Module saved = moduleRepository.save(module);
        return ModuleResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteModule(UUID courseId, UUID moduleId, UUID requesterId, List<String> requesterRoles) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);

        Module module = moduleRepository.findByIdAndCourseId(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));

        moduleRepository.delete(module);
        log.info("Deleted module {} from course {}", moduleId, courseId);

        recalculateCourseDuration(course);
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> getModulesByCourse(UUID courseId) {
        return moduleRepository.findByCourseIdOrderByPositionAsc(courseId)
                .stream()
                .map(ModuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ModuleResponse> reorderModules(UUID courseId, UUID requesterId, List<String> requesterRoles, ReorderPositionsRequest request) {
        verifyCourseOwnership(courseId, requesterId, requesterRoles);

        List<Module> modules = moduleRepository.findByCourseIdOrderByPositionAsc(courseId);
        Map<UUID, Module> moduleMap = modules.stream().collect(Collectors.toMap(Module::getId, m -> m));

        for (ReorderPositionsRequest.ItemPosition item : request.getItems()) {
            Module mod = moduleMap.get(item.getId());
            if (mod != null) {
                mod.setPosition(item.getPosition());
            }
        }

        moduleRepository.saveAll(modules);
        log.info("Reordered {} modules for course {}", request.getItems().size(), courseId);

        return moduleRepository.findByCourseIdOrderByPositionAsc(courseId)
                .stream()
                .map(ModuleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public LessonResponse addLesson(UUID courseId, UUID moduleId, UUID requesterId, List<String> requesterRoles, CreateLessonRequest request) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);

        Module module = moduleRepository.findByIdAndCourseId(moduleId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));

        int nextPosition = lessonRepository.findMaxPositionByModuleId(moduleId) + 1;

        Lesson lesson = Lesson.builder()
                .module(module)
                .courseId(courseId)
                .title(request.getTitle())
                .type(request.getType())
                .content(request.getContent())
                .mediaId(request.getMediaId())
                .durationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0)
                .position(nextPosition)
                .isFreePreview(Boolean.TRUE.equals(request.getIsFreePreview()))
                .isPublished(true)
                .build();

        Lesson saved = lessonRepository.save(lesson);
        log.info("Added lesson {} to module {}", saved.getId(), moduleId);

        recalculateCourseDuration(course);
        return LessonResponse.fromEntity(saved);
    }

    @Transactional
    public LessonResponse updateLesson(UUID courseId, UUID moduleId, UUID lessonId, UUID requesterId, List<String> requesterRoles, UpdateLessonRequest request) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);

        Lesson lesson = lessonRepository.findByIdAndModuleId(lessonId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        if (request.getTitle() != null) {
            lesson.setTitle(request.getTitle());
        }
        if (request.getType() != null) {
            lesson.setType(request.getType());
        }
        if (request.getContent() != null) {
            lesson.setContent(request.getContent());
        }
        if (request.getMediaId() != null) {
            lesson.setMediaId(request.getMediaId());
        }
        if (request.getDurationSeconds() != null) {
            lesson.setDurationSeconds(request.getDurationSeconds());
        }
        if (request.getIsFreePreview() != null) {
            lesson.setIsFreePreview(request.getIsFreePreview());
        }
        if (request.getIsPublished() != null) {
            lesson.setIsPublished(request.getIsPublished());
        }

        Lesson saved = lessonRepository.save(lesson);

        if (request.getDurationSeconds() != null) {
            recalculateCourseDuration(course);
        }

        return LessonResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteLesson(UUID courseId, UUID moduleId, UUID lessonId, UUID requesterId, List<String> requesterRoles) {
        Course course = verifyCourseOwnership(courseId, requesterId, requesterRoles);

        Lesson lesson = lessonRepository.findByIdAndModuleId(lessonId, moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        lessonRepository.delete(lesson);
        log.info("Deleted lesson {} from module {}", lessonId, moduleId);

        recalculateCourseDuration(course);
    }

    @Transactional
    public List<LessonResponse> reorderLessons(UUID courseId, UUID moduleId, UUID requesterId, List<String> requesterRoles, ReorderPositionsRequest request) {
        verifyCourseOwnership(courseId, requesterId, requesterRoles);

        List<Lesson> lessons = lessonRepository.findByModuleIdOrderByPositionAsc(moduleId);
        Map<UUID, Lesson> lessonMap = lessons.stream().collect(Collectors.toMap(Lesson::getId, l -> l));

        for (ReorderPositionsRequest.ItemPosition item : request.getItems()) {
            Lesson les = lessonMap.get(item.getId());
            if (les != null) {
                les.setPosition(item.getPosition());
            }
        }

        lessonRepository.saveAll(lessons);
        log.info("Reordered {} lessons for module {}", request.getItems().size(), moduleId);

        return lessonRepository.findByModuleIdOrderByPositionAsc(moduleId)
                .stream()
                .map(LessonResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void recalculateCourseDuration(Course course) {
        Integer totalDuration = lessonRepository.sumDurationByCourseId(course.getId());
        course.setTotalDurationSeconds(totalDuration != null ? totalDuration : 0);
        courseRepository.save(course);
    }
}
