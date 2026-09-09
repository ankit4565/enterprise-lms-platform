package com.enterprise.courseservice.entity;

import com.enterprise.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attempt_answers", uniqueConstraints = {
        @UniqueConstraint(name = "uq_attempt_question", columnNames = {"attempt_id", "question_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(name = "selected_option_ids", columnDefinition = "TEXT")
    private String selectedOptionIds;

    @Column(name = "text_answer", columnDefinition = "TEXT")
    private String textAnswer;

    @Column(name = "numeric_answer", precision = 10, scale = 4)
    private BigDecimal numericAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "marks_awarded", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal marksAwarded = BigDecimal.ZERO;

    @Column(name = "answered_at", nullable = false)
    @Builder.Default
    private Instant answeredAt = Instant.now();
}
