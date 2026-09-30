package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 그룹 목표 하나를 멤버 한 명이 달성했다는 기록 (목표 하나당 멤버 한 명당 최대 한 줄)
@Entity
@Table(name = "study_group_goal_achievements")
public class StudyGroupGoalAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goal_id", nullable = false)
    private Long goalId;

    @Column(nullable = false)
    private String username;

    @Column(name = "achieved_at", nullable = false, updatable = false)
    private LocalDateTime achievedAt = LocalDateTime.now();

    protected StudyGroupGoalAchievement() {
        // JPA 기본 생성자
    }

    public static StudyGroupGoalAchievement create(Long goalId, String username) {
        StudyGroupGoalAchievement achievement = new StudyGroupGoalAchievement();
        achievement.goalId = goalId;
        achievement.username = username;
        return achievement;
    }

    public Long getId() {
        return id;
    }

    public Long getGoalId() {
        return goalId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getAchievedAt() {
        return achievedAt;
    }
}