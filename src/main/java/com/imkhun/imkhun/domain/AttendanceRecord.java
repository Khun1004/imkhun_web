package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 학생 한 명의 강의(신청/Application) 하나에 대한 출석 기록 한 건
@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    // "PRESENT"(출석) / "ABSENT"(결석) / "MAKEUP"(보강)
    @Column(nullable = false)
    private String status;

    @Column
    private String note;

    // 학생이 스스로 "출석하기" 버튼을 눌러서 생긴 기록이면 true, 관리자가 직접 입력했으면 false
    @Column(name = "checked_in_by_student", nullable = false)
    private boolean checkedInByStudent = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AttendanceRecord() {
        // JPA 기본 생성자
    }

    public static AttendanceRecord create(Long applicationId, LocalDate classDate, String status, String note) {
        AttendanceRecord record = new AttendanceRecord();
        record.applicationId = applicationId;
        record.classDate = classDate;
        record.status = status;
        record.note = note;
        return record;
    }

    public static AttendanceRecord createByStudent(Long applicationId, LocalDate classDate, String status) {
        AttendanceRecord record = new AttendanceRecord();
        record.applicationId = applicationId;
        record.classDate = classDate;
        record.status = status;
        record.checkedInByStudent = true;
        return record;
    }

    public void updateStatus(String status) {
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public boolean isCheckedInByStudent() {
        return checkedInByStudent;
    }
}