package com.enterprise.courseservice.entity;

import com.enterprise.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_quiz_student_attempt", columnNames = {"quiz_id", "student_id", "attempt_no"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "attempt_no", nullable = false)
    @Builder.Default
    private Integer attemptNo = 1;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Column(precision = 6, scale = 2)
    private BigDecimal score;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    private Boolean passed;

    @Column(name = "correct_count")
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "wrong_count")
    @Builder.Default
    private Integer wrongCount = 0;

    @Column(name = "unanswered_count")
    @Builder.Default
    private Integer unansweredCount = 0;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AttemptAnswer> answers = new ArrayList<>();
}
