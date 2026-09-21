package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 발음 녹음해서 제출하는 것. 오디오는 base64 데이터 URI로 그대로 저장함 (영수증 이미지랑 같은 방식)
@Entity
@Table(name = "voice_submissions")
public class VoiceSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String courseName;

    // 학생이 붙이는 짧은 제목/맥락 (예: "1과 인사말 연습")
    @Column
    private String title;

    @Lob
    @Column(name = "audio_data", nullable = false, columnDefinition = "LONGTEXT")
    private String audioData;

    // 선생님 코멘트 — null이면 아직 안 들어봤다는 뜻
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String adminComment;

    @Column(name = "commented_at")
    private LocalDateTime commentedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected VoiceSubmission() {
        // JPA 기본 생성자
    }

    public static VoiceSubmission create(Long applicationId, String username, String courseName, String title, String audioData) {
        VoiceSubmission submission = new VoiceSubmission();
        submission.applicationId = applicationId;
        submission.username = username;
        submission.courseName = courseName;
        submission.title = title;
        submission.audioData = audioData;
        return submission;
    }

    public void addComment(String comment) {
        this.adminComment = comment;
        this.commentedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getUsername() {
        return username;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getTitle() {
        return title;
    }

    public String getAudioData() {
        return audioData;
    }

    public String getAdminComment() {
        return adminComment;
    }

    public LocalDateTime getCommentedAt() {
        return commentedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}