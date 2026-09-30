package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 관리자가 특정 학생의 강의(신청/Application)에 보내는 파일 — 자격증, 시험 자료 등
@Entity
@Table(name = "admin_sent_files")
public class AdminSentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    // "CERTIFICATE"(자격증) / "EXAM"(시험 자료)
    @Column(nullable = false)
    private String category;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    // base64 데이터 URI
    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "LONGTEXT")
    private String fileData;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ---- 시험 응시 (category가 "EXAM"인 파일에만 의미가 있음) ----
    // 학생이 "네, 계속 진행합니다"를 누른 시각 (null이면 아직 시작 안 함) — 여기서 1시간을 셈
    @Column(name = "exam_started_at")
    private LocalDateTime examStartedAt;

    // 학생이 "시험 완료하기"를 누른 시각 (null이면 아직 완료 안 함) — 한 번 채워지면 다시 응시 불가
    @Column(name = "exam_completed_at")
    private LocalDateTime examCompletedAt;

    protected AdminSentFile() {
        // JPA 기본 생성자
    }

    public static AdminSentFile create(Long applicationId, String category, String fileName, String fileData) {
        AdminSentFile file = new AdminSentFile();
        file.applicationId = applicationId;
        file.category = category;
        file.fileName = fileName;
        file.fileData = fileData;
        return file;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getCategory() {
        return category;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileData() {
        return fileData;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExamStartedAt() {
        return examStartedAt;
    }

    public LocalDateTime getExamCompletedAt() {
        return examCompletedAt;
    }

    // "네, 계속 진행합니다"를 누른 순간 호출. 이미 시작했으면(새로고침 등) 그대로 두고 무시함 —
    // 그래야 타이머가 처음 시작한 시각 기준으로 계속 흘러가요
    public void startExam() {
        if (this.examStartedAt == null) {
            this.examStartedAt = LocalDateTime.now();
        }
    }

    public void completeExam() {
        this.examCompletedAt = LocalDateTime.now();
    }
}