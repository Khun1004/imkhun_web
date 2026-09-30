package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 스터디 그룹 안에서 멤버들끼리 주고받는 짧은 노트(게시판 느낌)
@Entity
@Table(name = "study_group_notes")
public class StudyGroupNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "sender_username", nullable = false)
    private String senderUsername;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudyGroupNote() {
        // JPA 기본 생성자
    }

    public static StudyGroupNote create(Long groupId, String senderUsername, String message) {
        StudyGroupNote note = new StudyGroupNote();
        note.groupId = groupId;
        note.senderUsername = senderUsername;
        note.message = message;
        return note;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}