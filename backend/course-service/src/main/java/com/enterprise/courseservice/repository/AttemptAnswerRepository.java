package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.AttemptAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {

    Optional<AttemptAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    List<AttemptAnswer> findAllByAttemptId(UUID attemptId);
}
