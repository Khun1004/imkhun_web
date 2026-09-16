package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 학생 한 명(강의 신청 = Application 기준)의 레벨/실력 기록. 시간이 지나면서 어떻게 늘었는지
// 히스토리로 쌓아두는 용도라, 매번 "새 기록"을 추가하는 방식이지 덮어쓰지 않음.
@Entity
@Table(name = "student_level_records")
public class StudentLevelRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    // 자유 텍스트 (예: "초급", "중급 2", "TOPIK 3급") — 언어/과목마다 등급 체계가 달라서 고정 목록으로 안 만듦
    @Column(nullable = false)
    private String level;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String note;

    // 이 레벨로 평가/기록한 날짜 (선택 — 비워두면 오늘로 취급)
    @Column(name = "recorded_date")
    private LocalDate recordedDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudentLevelRecord() {
        // JPA 기본 생성자
    }

    public static StudentLevelRecord create(Long applicationId, String level, String note, LocalDate recordedDate) {
        StudentLevelRecord record = new StudentLevelRecord();
        record.applicationId = applicationId;
        record.level = level;
        record.note = note;
        record.recordedDate = recordedDate != null ? recordedDate : LocalDate.now();
        return record;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getLevel() {
        return level;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getRecordedDate() {
        return recordedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}