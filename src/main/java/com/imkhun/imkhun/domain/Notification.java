package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생 또는 관리자에게 보내는 알림 (댓글/답글, 신청 승인, 새 신청 등)
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "STUDENT" / "ADMIN"
    @Column(name = "recipient_type", nullable = false)
    private String recipientType;

    // 학생용 알림이면 학생 username, 관리자용이면 null (관리자는 한 명뿐이라 구분이 필요 없어요)
    @Column(name = "recipient_username")
    private String recipientUsername;

    // "COMMENT_REPLY" / "POST_COMMENT" / "ADMIN_COMMENT" / "APPLICATION_APPROVED" / "NEW_APPLICATION"
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String message;

    // 관련 게시글 id — 있으면 눌렀을 때 그 글로 이동할 수 있어요 (없으면 null)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Notification() {
        // JPA 기본 생성자
    }

    public static Notification forStudent(String username, String type, String message, Long postId) {
        Notification n = new Notification();
        n.recipientType = "STUDENT";
        n.recipientUsername = username;
        n.type = type;
        n.message = message;
        n.postId = postId;
        return n;
    }

    public static Notification forAdmin(String type, String message, Long postId) {
        Notification n = new Notification();
        n.recipientType = "ADMIN";
        n.type = type;
        n.message = message;
        n.postId = postId;
        return n;
    }

    public void markRead() {
        this.isRead = true;
    }

    public Long getId() {
        return id;
    }

    public String getRecipientType() {
        return recipientType;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public Long getPostId() {
        return postId;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}