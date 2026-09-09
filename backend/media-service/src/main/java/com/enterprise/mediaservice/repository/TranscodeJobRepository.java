package com.enterprise.mediaservice.repository;

import com.enterprise.mediaservice.entity.TranscodeJob;
import com.enterprise.mediaservice.entity.TranscodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TranscodeJobRepository extends JpaRepository<TranscodeJob, UUID> {

    List<TranscodeJob> findByMediaIdOrderByCreatedAtDesc(UUID mediaId);

    Optional<TranscodeJob> findTopByMediaIdOrderByCreatedAtDesc(UUID mediaId);

    List<TranscodeJob> findByStatus(TranscodeStatus status);
}
