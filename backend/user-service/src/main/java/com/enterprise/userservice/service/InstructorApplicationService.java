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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstructorApplicationService {

    private final InstructorApplicationRepository applicationRepository;

    @Transactional
    public InstructorApplicationResponse submitApplication(UUID userId, InstructorApplicationRequest request) {
        boolean hasPending = applicationRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING);
        if (hasPending) {
            throw new ApiException(ErrorCode.CONFLICT, "An instructor application is already pending review");
        }

        InstructorApplication application = InstructorApplication.builder()
                .userId(userId)
                .qualifications(request.getQualifications())
                .sampleUrl(request.getSampleUrl())
                .status(ApplicationStatus.PENDING)
                .build();

        InstructorApplication saved = applicationRepository.save(application);
        log.info("Instructor application submitted with id {} for user {}", saved.getId(), userId);
        return InstructorApplicationResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<InstructorApplicationResponse> getMyApplications(UUID userId) {
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(InstructorApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InstructorApplicationResponse> getAllApplications(ApplicationStatus status, Pageable pageable) {
        if (status != null) {
            return applicationRepository.findByStatus(status, pageable)
                    .map(InstructorApplicationResponse::fromEntity);
        }
        return applicationRepository.findAll(pageable)
                .map(InstructorApplicationResponse::fromEntity);
    }

    @Transactional
    public InstructorApplicationResponse reviewApplication(UUID applicationId, UUID reviewerId, ReviewApplicationRequest request) {
        InstructorApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Instructor application not found with id: " + applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new ApiException(ErrorCode.CONFLICT, "Application has already been reviewed with status: " + application.getStatus());
        }

        application.setStatus(request.getStatus());
        application.setReviewedBy(reviewerId);
        application.setReviewNote(request.getReviewNote());
        application.setReviewedAt(Instant.now());

        InstructorApplication saved = applicationRepository.save(application);
        log.info("Instructor application {} reviewed by {} with outcome {}", applicationId, reviewerId, request.getStatus());
        return InstructorApplicationResponse.fromEntity(saved);
    }
}
