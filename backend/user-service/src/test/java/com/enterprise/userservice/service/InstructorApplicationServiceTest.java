package com.enterprise.userservice.service;

import com.enterprise.common.exception.ApiException;
import com.enterprise.common.exception.ErrorCode;
import com.enterprise.common.exception.ResourceNotFoundException;
import com.enterprise.userservice.dto.request.InstructorApplicationRequest;
import com.enterprise.userservice.dto.request.ReviewApplicationRequest;
import com.enterprise.userservice.dto.response.InstructorApplicationResponse;
import com.enterprise.userservice.entity.ApplicationStatus;
import com.enterprise.userservice.entity.InstructorApplication;
import com.enterprise.userservice.repository.InstructorApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorApplicationServiceTest {

    @Mock
    private InstructorApplicationRepository applicationRepository;

    @InjectMocks
    private InstructorApplicationService applicationService;

    private UUID userId;
    private UUID appId;
    private InstructorApplication application;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        appId = UUID.randomUUID();
        application = InstructorApplication.builder()
                .id(appId)
                .userId(userId)
                .status(ApplicationStatus.PENDING)
                .qualifications("PhD in Computer Science with 10 years teaching experience")
                .sampleUrl("https://youtube.com/sample-lecture")
                .build();
    }

    @Test
    void submitApplication_Success() {
        when(applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)).thenReturn(false);
        when(applicationRepository.save(any(InstructorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InstructorApplicationRequest request = InstructorApplicationRequest.builder()
                .qualifications("PhD in CS")
                .sampleUrl("https://youtube.com/sample")
                .build();

        InstructorApplicationResponse response = applicationService.submitApplication(userId, request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(ApplicationStatus.PENDING, response.getStatus());
        assertEquals("PhD in CS", response.getQualifications());
        verify(applicationRepository).save(any(InstructorApplication.class));
    }

    @Test
    void submitApplication_DuplicatePending_ThrowsConflict() {
        when(applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING)).thenReturn(true);

        InstructorApplicationRequest request = InstructorApplicationRequest.builder()
                .qualifications("PhD in CS")
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                applicationService.submitApplication(userId, request)
        );

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void getMyApplications_ReturnsList() {
        when(applicationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(application));

        List<InstructorApplicationResponse> list = applicationService.getMyApplications(userId);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(appId, list.get(0).getId());
    }

    @Test
    void getAllApplications_WithStatus_FiltersByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        when(applicationRepository.findByStatus(ApplicationStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(application), pageable, 1));

        Page<InstructorApplicationResponse> page = applicationService.getAllApplications(ApplicationStatus.PENDING, pageable);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(ApplicationStatus.PENDING, page.getContent().get(0).getStatus());
    }

    @Test
    void reviewApplication_Success_Approves() {
        UUID reviewerId = UUID.randomUUID();
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(InstructorApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewApplicationRequest request = ReviewApplicationRequest.builder()
                .status(ApplicationStatus.APPROVED)
                .reviewNote("Excellent credentials")
                .build();

        InstructorApplicationResponse response = applicationService.reviewApplication(appId, reviewerId, request);

        assertNotNull(response);
        assertEquals(ApplicationStatus.APPROVED, response.getStatus());
        assertEquals(reviewerId, response.getReviewedBy());
        assertEquals("Excellent credentials", response.getReviewNote());
        assertNotNull(response.getReviewedAt());
        verify(applicationRepository).save(application);
    }

    @Test
    void reviewApplication_AlreadyReviewed_ThrowsConflict() {
        application.setStatus(ApplicationStatus.APPROVED);
        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        ReviewApplicationRequest request = ReviewApplicationRequest.builder()
                .status(ApplicationStatus.REJECTED)
                .build();

        ApiException ex = assertThrows(ApiException.class, () ->
                applicationService.reviewApplication(appId, UUID.randomUUID(), request)
        );

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void reviewApplication_NotFound_ThrowsResourceNotFoundException() {
        when(applicationRepository.findById(appId)).thenReturn(Optional.empty());

        ReviewApplicationRequest request = ReviewApplicationRequest.builder()
                .status(ApplicationStatus.APPROVED)
                .build();

        assertThrows(ResourceNotFoundException.class, () ->
                applicationService.reviewApplication(appId, UUID.randomUUID(), request)
        );
    }
}
