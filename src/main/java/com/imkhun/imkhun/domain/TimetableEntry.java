package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 공개 홈페이지 "공지사항" 탭에 나오는 강의 시간표의 칸 하나
@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "MON" / "TUE" / "WED" / "THU" / "FRI"
    // 컬럼명을 day_of_week로 지정함 — "day"는 H2에서 예약어라 그대로 쓰면 SQL 오류가 남
    @Column(name = "day_of_week", nullable = false)
    private String day;

    // "14:00" 같은 문자열 그대로 저장 (자유 형식)
    @Column(name = "start_time", nullable = false)
    private String startTime;

    @Column(name = "end_time", nullable = false)
    private String endTime;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    // "korean" / "computer" / "other" — 표에서 색깔 구분용
    @Column(name = "color_type", nullable = false)
    private String colorType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected TimetableEntry() {
        // JPA 기본 생성자
    }

    public static TimetableEntry create(String day, String startTime, String endTime, String courseName, String colorType) {
        TimetableEntry entry = new TimetableEntry();
        entry.day = day;
        entry.startTime = startTime;
        entry.endTime = endTime;
        entry.courseName = courseName;
        entry.colorType = colorType;
        return entry;
    }

    public void update(String day, String startTime, String endTime, String courseName, String colorType) {
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.courseName = courseName;
        this.colorType = colorType;
    }

    public Long getId() {
        return id;
    }

    public String getDay() {
        return day;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getCourseName() {
        return courseName;
    }

    public String getColorType() {
        return colorType;
    }
}