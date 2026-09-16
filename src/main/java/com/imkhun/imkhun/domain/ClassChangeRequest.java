package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 학생이 "이 날 수업 취소하고 싶어요" 또는 "이 날 대신 다른 날로 바꾸고 싶어요"라고 보내는 요청
@Entity
@Table(name = "class_change_requests")
public class ClassChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    // "CANCEL"(취소) / "RESCHEDULE"(변경)
    @Column(name = "request_type", nullable = false)
    private String requestType;

    // 원래 예정되어 있던 수업 날짜
    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    // RESCHEDULE일 때만 씀 — 학생이 희망하는 새 날짜
    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String reason;

    // "PENDING"(대기중) / "APPROVED"(승인) / "REJECTED"(거절)
    @Column(nullable = false)
    private String status = "PENDING";

    @Lob
    @Column(name = "admin_reply", columnDefinition = "LONGTEXT")
    private String adminReply;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    protected ClassChangeRequest() {
        // JPA 기본 생성자
    }

    public static ClassChangeRequest create(Long applicationId, String requestType, LocalDate classDate,
                                            LocalDate requestedDate, String reason) {
        ClassChangeRequest request = new ClassChangeRequest();
        request.applicationId = applicationId;
        request.requestType = requestType;
        request.classDate = classDate;
        request.requestedDate = requestedDate;
        request.reason = reason;
        return request;
    }

    public void respond(String status, String adminReply) {
        this.status = status;
        this.adminReply = adminReply;
        this.respondedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getRequestType() {
        return requestType;
    }

    public LocalDate getClassDate() {
        return classDate;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
}