package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 숙제에 실제로 낸 답. 글로 쓴 답이나 파일(사진 등)을 첨부할 수 있음
@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false, unique = true)
    private Long assignmentId;

    @Column(nullable = false)
    private String username;

    @Lob
    @Column(name = "text_answer", columnDefinition = "LONGTEXT")
    private String textAnswer;

    // base64 데이터 URI 여러 개를 JSON 배열로 저장 (예: [{"data":"...","name":"..."}]) — 없으면 null
    @Lob
    @Column(name = "attachments_json", columnDefinition = "LONGTEXT")
    private String attachmentsJson;

    // 선생님 코멘트 — null이면 아직 안 봤다는 뜻
    @Lob
    @Column(name = "admin_comment", columnDefinition = "LONGTEXT")
    private String adminComment;

    @Column(name = "commented_at")
    private LocalDateTime commentedAt;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    protected AssignmentSubmission() {
        // JPA 기본 생성자
    }

    public static AssignmentSubmission create(Long assignmentId, String username, String textAnswer, String attachmentsJson) {
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.assignmentId = assignmentId;
        submission.username = username;
        submission.textAnswer = textAnswer;
        submission.attachmentsJson = attachmentsJson;
        return submission;
    }

    public void updateContent(String textAnswer, String attachmentsJson) {
        this.textAnswer = textAnswer;
        if (attachmentsJson != null) {
            this.attachmentsJson = attachmentsJson;
        }
        this.submittedAt = LocalDateTime.now();
    }

    public void addComment(String comment) {
        this.adminComment = comment;
        this.commentedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public String getUsername() {
        return username;
    }

    public String getTextAnswer() {
        return textAnswer;
    }

    public String getAttachmentsJson() {
        return attachmentsJson;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public LocalDateTime getCommentedAt() {
        return commentedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}