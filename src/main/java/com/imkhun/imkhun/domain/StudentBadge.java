package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 획득한 배지 기록 (한 번 획득하면 계속 유지됨 — 조건을 다시 못 채워도 사라지지 않음)
@Entity
@Table(name = "student_badges", uniqueConstraints = @UniqueConstraint(columnNames = {"username", "badge_key"}))
public class StudentBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    // BadgeCatalog에 정의된 배지 고유 키 (예: "ATTEND_STREAK_3")
    @Column(name = "badge_key", nullable = false)
    private String badgeKey;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt = LocalDateTime.now();

    protected StudentBadge() {
        // JPA 기본 생성자
    }

    public static StudentBadge create(String username, String badgeKey) {
        StudentBadge badge = new StudentBadge();
        badge.username = username;
        badge.badgeKey = badgeKey;
        return badge;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getBadgeKey() {
        return badgeKey;
    }

    public LocalDateTime getEarnedAt() {
        return earnedAt;
    }
}