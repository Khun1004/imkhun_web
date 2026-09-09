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
    @Column(name = "file_data", nullable = false, columnDefinition = "CLOB")
    private String fileData;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

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
}