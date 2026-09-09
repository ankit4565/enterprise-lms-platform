package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, UUID> {

    List<QuizQuestion> findAllByQuizIdOrderByPositionAsc(UUID quizId);

    long countByQuizId(UUID quizId);
}
