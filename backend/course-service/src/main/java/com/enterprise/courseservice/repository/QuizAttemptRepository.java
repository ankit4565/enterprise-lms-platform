package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.AttemptStatus;
import com.enterprise.courseservice.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    Optional<QuizAttempt> findTopByQuizIdAndStudentIdOrderByAttemptNoDesc(UUID quizId, UUID studentId);

    List<QuizAttempt> findAllByQuizIdAndStudentIdOrderByAttemptNoAsc(UUID quizId, UUID studentId);

    Optional<QuizAttempt> findByQuizIdAndStudentIdAndStatus(UUID quizId, UUID studentId, AttemptStatus status);

    long countByQuizIdAndStudentId(UUID quizId, UUID studentId);

    boolean existsByQuizIdAndStudentIdAndStatus(UUID quizId, UUID studentId, AttemptStatus status);
}
