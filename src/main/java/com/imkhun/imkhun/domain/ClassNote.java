package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 학생이 수업 하나(출석 기록 하나)를 듣고 나서 남기는 짧은 개인 노트.
// 출석 기록(AttendanceRecord) 하나에 노트는 하나만 붙을 수 있음 (덮어쓰기 방식).
@Entity
@Table(name = "class_notes")
public class ClassNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "attendance_record_id", nullable = false, unique = true)
    private Long attendanceRecordId;

    // 목록에 바로 보여주기 위해 저장해두는 값 (조회할 때마다 강의 정보를 다시 조인하지 않도록)
    @Column(name = "course_name", nullable = false)
    private String courseName;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected ClassNote() {
        // JPA 기본 생성자
    }

    public static ClassNote create(String username, Long attendanceRecordId, String courseName, LocalDate classDate, String content) {
        ClassNote note = new ClassNote();
        note.username = username;
        note.attendanceRecordId = attendanceRecordId;
        note.courseName = courseName;
        note.classDate = classDate;
        note.content = content;
        return note;
    }

    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Long getAttendanceRecordId() {
        return attendanceRecordId;
    }

    public String getCourseName() {
        return courseName;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}