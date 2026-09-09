package com.enterprise.courseservice.entity;

import com.enterprise.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal marks = BigDecimal.ONE;

    @Column(name = "negative_marks", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal negativeMarks = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    @Builder.Default
    private Integer position = 0;

    @Column(name = "correct_text", columnDefinition = "TEXT")
    private String correctText;

    @Column(name = "numeric_answer", precision = 10, scale = 4)
    private BigDecimal numericAnswer;

    @Column(precision = 8, scale = 4)
    @Builder.Default
    private BigDecimal tolerance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<QuizOption> options = new ArrayList<>();
}
