package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 학생 한 명(Application 기준)에게 내준 숙제 한 건
@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    // 제출 기한 (선택)
    @Column(name = "due_date")
    private LocalDate dueDate;

    // 학생이 "완료했어요" 체크했는지
    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Assignment() {
        // JPA 기본 생성자
    }

    public static Assignment create(Long applicationId, String title, String description, LocalDate dueDate) {
        Assignment assignment = new Assignment();
        assignment.applicationId = applicationId;
        assignment.title = title;
        assignment.description = description;
        assignment.dueDate = dueDate;
        return assignment;
    }

    public void markComplete() {
        this.completed = true;
        this.completedAt = LocalDateTime.now();
    }

    public void markIncomplete() {
        this.completed = false;
        this.completedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}