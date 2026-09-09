package com.enterprise.mediaservice.repository;

import com.enterprise.mediaservice.entity.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {

    Optional<UploadSession> findByMediaId(UUID mediaId);

    Optional<UploadSession> findByUploadId(String uploadId);
}
