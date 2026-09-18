package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생 한 명이 단어 하나를 "외웠는지"를 기록함. username+wordId 조합 하나당 한 행만 있음
@Entity
@Table(name = "vocabulary_progress")
public class VocabularyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @Column(nullable = false)
    private boolean learned;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected VocabularyProgress() {
        // JPA 기본 생성자
    }

    public static VocabularyProgress create(String username, Long wordId, boolean learned) {
        VocabularyProgress progress = new VocabularyProgress();
        progress.username = username;
        progress.wordId = wordId;
        progress.learned = learned;
        return progress;
    }

    public void update(boolean learned) {
        this.learned = learned;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Long getWordId() {
        return wordId;
    }

    public boolean isLearned() {
        return learned;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}