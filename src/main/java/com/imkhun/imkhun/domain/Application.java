package com.imkhun.imkhun.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신청한 사용자 아이디 (User.username)
    @Column(nullable = false)
    private String username;

    // "TOGETHER"(나랑 같이 공부하기) 또는 "VIDEO"(영상으로만 듣기)
    @Column(nullable = false)
    private String studyType;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private String contact;

    @Column
    private String memo;

    // "PENDING"(승인대기) 또는 "APPROVED"(승인완료) — 지금은 항상 PENDING으로 시작
    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 승인될 때 자동으로 생성됨 (예: 2026_Japanese_Level1_01) — 승인 전엔 null
    @Column
    private String studentNumber;

    protected Application() {
        // JPA 기본 생성자
    }

    public static Application create(String username, String studyType, String courseName,
                                     String contact, String memo) {
        Application application = new Application();
        application.username = username;
        application.studyType = studyType;
        application.courseName = courseName;
        application.contact = contact;
        application.memo = memo;
        application.status = "PENDING";
        return application;
    }

    public void changeStatus(String status) {
        this.status = status;
    }

    public void assignStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getStudyType() {
        return studyType;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getContact() {
        return contact;
    }

    public String getMemo() {
        return memo;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getStudentNumber() {
        return studentNumber;
    }
}