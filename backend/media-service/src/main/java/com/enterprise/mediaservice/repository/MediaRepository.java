package com.enterprise.mediaservice.repository;

import com.enterprise.mediaservice.entity.Media;
import com.enterprise.mediaservice.entity.MediaContextType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    Optional<Media> findByIdAndIsDeletedFalse(UUID id);

    List<Media> findByOwnerIdAndIsDeletedFalse(UUID ownerId);

    List<Media> findByContextTypeAndContextIdAndIsDeletedFalse(MediaContextType contextType, UUID contextId);
}
