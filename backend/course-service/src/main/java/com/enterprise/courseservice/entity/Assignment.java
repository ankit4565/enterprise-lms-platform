package com.enterprise.courseservice.entity;

import com.enterprise.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false, unique = true)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String instructions;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Integer maxScore = 100;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "allow_late", nullable = false)
    @Builder.Default
    private Boolean allowLate = true;

    @Column(name = "late_penalty_percent", nullable = false)
    @Builder.Default
    private Integer latePenaltyPercent = 0;

    @Column(name = "allowed_file_types", length = 255)
    private String allowedFileTypes;

    @Column(name = "max_file_size_mb", nullable = false)
    @Builder.Default
    private Integer maxFileSizeMb = 50;
}
