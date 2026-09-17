package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 공개 홈페이지 "공지사항" 탭에 나오는 글
@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    // 비워두면 바로 공개, 값이 있으면 그 시간이 될 때까지는 학생들한테 안 보이고 알림도 안 감
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    // 실제로 알림이 나간 시각 — null이면 아직 예약 대기중이거나(scheduledAt이 미래) 즉시 공개된 글임
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Notice() {
        // JPA 기본 생성자
    }

    public static Notice create(String title, String content, LocalDateTime scheduledAt) {
        Notice notice = new Notice();
        notice.title = title;
        notice.content = content;
        notice.scheduledAt = scheduledAt;
        // 예약 없이 바로 올리는 글은 알림도 바로 나간 것으로 침 (관리자가 즉시 올린 거니까)
        if (scheduledAt == null) {
            notice.notifiedAt = LocalDateTime.now();
        }
        return notice;
    }

    public void update(String title, String content, LocalDateTime scheduledAt) {
        this.title = title;
        this.content = content;
        this.scheduledAt = scheduledAt;
    }

    public void markNotified() {
        this.notifiedAt = LocalDateTime.now();
    }

    // 지금 시점에 공개(학생한테 보임)되어야 하는 글인지 — 예약 시간이 없거나 이미 지났으면 공개
    public boolean isPublishedNow() {
        return scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public LocalDateTime getNotifiedAt() {
        return notifiedAt;
    }
}