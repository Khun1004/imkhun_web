package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 직접 정하는 개인 학습 목표. 진행률은 저장하지 않고, 조회할 때마다 실제 데이터(출석/단어/숙제)로 계산함
@Entity
@Table(name = "learning_goals")
public class LearningGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    // ATTENDANCE_STREAK / ATTENDANCE_COUNT / VOCAB_WORDS / ASSIGNMENT_COUNT
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected LearningGoal() {
        // JPA 기본 생성자
    }

    public static LearningGoal create(String username, String type, String title, int targetValue) {
        LearningGoal goal = new LearningGoal();
        goal.username = username;
        goal.type = type;
        goal.title = title;
        goal.targetValue = targetValue;
        return goal;
    }

    public void markAchieved() {
        if (this.achievedAt == null) {
            this.achievedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public LocalDateTime getAchievedAt() {
        return achievedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}