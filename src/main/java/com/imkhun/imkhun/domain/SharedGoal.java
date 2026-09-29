package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 친구 두 명이 "같이" 도전하는 학습 목표. 개인 목표(LearningGoal)와 같은 종류(출석/단어/숙제)를
// 두 사람이 각자 채우고, 둘 다 목표에 도달하면 같이 축하 알림을 받음
@Entity
@Table(name = "shared_goals")
public class SharedGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String creatorUsername;

    @Column(nullable = false)
    private String partnerUsername;

    // ATTENDANCE_STREAK / ATTENDANCE_COUNT / VOCAB_WORDS / ASSIGNMENT_COUNT
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    // PENDING(제안함) / ACTIVE(둘 다 시작함) / DECLINED(거절함)
    @Column(nullable = false)
    private String status;

    @Column(name = "creator_achieved_at")
    private LocalDateTime creatorAchievedAt;

    @Column(name = "partner_achieved_at")
    private LocalDateTime partnerAchievedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected SharedGoal() {
        // JPA 기본 생성자
    }

    public static SharedGoal create(String creatorUsername, String partnerUsername, String type, String title, int targetValue) {
        SharedGoal goal = new SharedGoal();
        goal.creatorUsername = creatorUsername;
        goal.partnerUsername = partnerUsername;
        goal.type = type;
        goal.title = title;
        goal.targetValue = targetValue;
        goal.status = "PENDING";
        return goal;
    }

    public void accept() {
        this.status = "ACTIVE";
        this.acceptedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = "DECLINED";
    }

    public void markCreatorAchieved() {
        if (this.creatorAchievedAt == null) {
            this.creatorAchievedAt = LocalDateTime.now();
        }
    }

    public void markPartnerAchieved() {
        if (this.partnerAchievedAt == null) {
            this.partnerAchievedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public String getPartnerUsername() {
        return partnerUsername;
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

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatorAchievedAt() {
        return creatorAchievedAt;
    }

    public LocalDateTime getPartnerAchievedAt() {
        return partnerAchievedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}