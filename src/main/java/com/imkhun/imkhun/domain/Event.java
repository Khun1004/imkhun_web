package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// KWZM Center 홈 화면 "이벤트 & 행사" 카드에 나오는 글 — 관리자가 등록하고 학생만 볼 수 있음
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    // 행사/이벤트 날짜 (화면에 "2026.03.15" 식으로 보여줌)
    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Event() {
        // JPA 기본 생성자
    }

    public static Event create(String title, String content, LocalDate eventDate) {
        Event event = new Event();
        event.title = title;
        event.content = content;
        event.eventDate = eventDate;
        return event;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}