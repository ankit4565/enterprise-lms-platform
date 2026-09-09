package com.enterprise.courseservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.courseservice.dto.request.*;
import com.enterprise.courseservice.dto.response.LessonResponse;
import com.enterprise.courseservice.dto.response.ModuleResponse;
import com.enterprise.courseservice.entity.*;
import com.enterprise.courseservice.entity.Module;
import com.enterprise.courseservice.repository.CourseRepository;
import com.enterprise.courseservice.repository.LessonRepository;
import com.enterprise.courseservice.repository.ModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModuleLessonServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private ModuleLessonService moduleLessonService;

    private UUID courseId;
    private UUID instructorId;
    private Course course;
    private UUID moduleId;
    private Module module;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        instructorId = UUID.randomUUID();
        course = Course.builder()
                .id(courseId)
                .instructorId(instructorId)
                .title("Test Course")
                .status(CourseStatus.DRAFT)
                .totalDurationSeconds(0)
                .build();

        moduleId = UUID.randomUUID();
        module = Module.builder()
                .id(moduleId)
                .course(course)
                .title("Module 1")
                .position(1)
                .lessons(new ArrayList<>())
                .build();
    }

    @Test
    void addModule_Success() {
        CreateModuleRequest request = CreateModuleRequest.builder()
                .title("Introduction")
                .description("Getting started")
                .build();

        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(moduleRepository.findMaxPositionByCourseId(courseId)).thenReturn(0);
        when(moduleRepository.save(any(Module.class))).thenAnswer(invocation -> {
            Module m = invocation.getArgument(0);
            m.setId(moduleId);
            return m;
        });

        ModuleResponse response = moduleLessonService.addModule(courseId, instructorId, List.of("INSTRUCTOR"), request);

        assertNotNull(response);
        assertEquals("Introduction", response.getTitle());
        assertEquals(1, response.getPosition());
        verify(moduleRepository).save(any(Module.class));
    }

    @Test
    void addModule_UnauthorizedUser_ThrowsApiException() {
        UUID strangerId = UUID.randomUUID();
        CreateModuleRequest request = CreateModuleRequest.builder().title("Intro").build();

        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));

        assertThrows(ApiException.class, () ->
                moduleLessonService.addModule(courseId, strangerId, List.of("INSTRUCTOR"), request));
    }

    @Test
    void addLesson_Success_RecalculatesDuration() {
        CreateLessonRequest request = CreateLessonRequest.builder()
                .title("Lesson 1: Basics")
                .type(LessonType.VIDEO)
                .durationSeconds(300)
                .build();

        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(moduleRepository.findByIdAndCourseId(moduleId, courseId)).thenReturn(Optional.of(module));
        when(lessonRepository.findMaxPositionByModuleId(moduleId)).thenReturn(0);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson l = invocation.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });
        when(lessonRepository.sumDurationByCourseId(courseId)).thenReturn(300);

        LessonResponse response = moduleLessonService.addLesson(courseId, moduleId, instructorId, List.of("INSTRUCTOR"), request);

        assertNotNull(response);
        assertEquals("Lesson 1: Basics", response.getTitle());
        assertEquals(300, response.getDurationSeconds());
        assertEquals(1, response.getPosition());

        verify(courseRepository).save(course);
        assertEquals(300, course.getTotalDurationSeconds());
    }

    @Test
    void reorderModules_Success() {
        UUID mod2Id = UUID.randomUUID();
        Module mod2 = Module.builder().id(mod2Id).course(course).title("Module 2").position(2).build();

        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(moduleRepository.findByCourseIdOrderByPositionAsc(courseId)).thenReturn(List.of(module, mod2));

        ReorderPositionsRequest request = ReorderPositionsRequest.builder()
                .items(List.of(
                        new ReorderPositionsRequest.ItemPosition(moduleId, 2),
                        new ReorderPositionsRequest.ItemPosition(mod2Id, 1)
                ))
                .build();

        List<ModuleResponse> responses = moduleLessonService.reorderModules(courseId, instructorId, List.of("INSTRUCTOR"), request);

        assertNotNull(responses);
        verify(moduleRepository).saveAll(anyList());
    }

    @Test
    void deleteLesson_Success_RecalculatesDuration() {
        UUID lessonId = UUID.randomUUID();
        Lesson lesson = Lesson.builder().id(lessonId).module(module).courseId(courseId).title("To Delete").durationSeconds(120).build();

        when(courseRepository.findByIdAndDeletedAtIsNull(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.findByIdAndModuleId(lessonId, moduleId)).thenReturn(Optional.of(lesson));
        when(lessonRepository.sumDurationByCourseId(courseId)).thenReturn(0);

        moduleLessonService.deleteLesson(courseId, moduleId, lessonId, instructorId, List.of("INSTRUCTOR"));

        verify(lessonRepository).delete(lesson);
        verify(courseRepository).save(course);
        assertEquals(0, course.getTotalDurationSeconds());
    }
}
