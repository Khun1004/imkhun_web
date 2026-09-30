package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 스터디 그룹 전체가 같이 도전하는 목표. 활동 중인 멤버 전원이 각자 목표치에 도달하면
// 다같이 축하 알림을 받음 (completedAt으로 중복 알림 방지)
@Entity
@Table(name = "study_group_goals")
public class StudyGroupGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    // ATTENDANCE_STREAK / ATTENDANCE_COUNT / VOCAB_WORDS / ASSIGNMENT_COUNT
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @Column(name = "created_by_username", nullable = false)
    private String createdByUsername;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudyGroupGoal() {
        // JPA 기본 생성자
    }

    public static StudyGroupGoal create(Long groupId, String type, String title, int targetValue, String createdByUsername) {
        StudyGroupGoal goal = new StudyGroupGoal();
        goal.groupId = groupId;
        goal.type = type;
        goal.title = title;
        goal.targetValue = targetValue;
        goal.createdByUsername = createdByUsername;
        return goal;
    }

    public void markCompleted() {
        if (this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
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

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}