package com.imkhun.imkhun.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "study_notes")
public class StudyNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "korean" / "japanese" / "thai" / "english"
    @Column(nullable = false, unique = true)
    private String language;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String content;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected StudyNote() {
        // JPA 기본 생성자
    }

    public static StudyNote create(String language, String content) {
        StudyNote note = new StudyNote();
        note.language = language;
        note.content = content;
        note.updatedAt = LocalDateTime.now();
        return note;
    }

    public void updateContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}