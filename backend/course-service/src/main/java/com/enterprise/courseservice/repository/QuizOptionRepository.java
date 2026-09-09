package com.enterprise.courseservice.repository;

import com.enterprise.courseservice.entity.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, UUID> {

    List<QuizOption> findAllByQuestionIdOrderByPositionAsc(UUID questionId);
}
