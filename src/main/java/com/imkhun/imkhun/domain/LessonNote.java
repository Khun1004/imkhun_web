package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 학생 한 명(강의 신청 = Application 기준)에 대한 수업 진도 기록 한 건.
// "출석했는지"는 AttendanceRecord가 담당하고, 이건 "그날 뭘 배웠는지" 내용을 담아요.
@Entity
@Table(name = "lesson_notes")
public class LessonNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    // 어느 날 수업 내용인지 (선택 — 비워두면 그냥 메모로 씀)
    @Column(name = "class_date")
    private LocalDate classDate;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected LessonNote() {
        // JPA 기본 생성자
    }

    public static LessonNote create(Long applicationId, LocalDate classDate, String content) {
        LessonNote note = new LessonNote();
        note.applicationId = applicationId;
        note.classDate = classDate;
        note.content = content;
        return note;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public LocalDate getClassDate() {
        return classDate;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}