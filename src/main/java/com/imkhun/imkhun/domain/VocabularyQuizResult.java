package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 특정 Part의 퀴즈를 완료한 기록
@Entity
@Table(name = "vocabulary_quiz_results")
public class VocabularyQuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_id", nullable = false)
    private Long setId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int score;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt = LocalDateTime.now();

    protected VocabularyQuizResult() {
        // JPA 기본 생성자
    }

    public static VocabularyQuizResult create(Long setId, String username, int score, int totalQuestions) {
        VocabularyQuizResult result = new VocabularyQuizResult();
        result.setId = setId;
        result.username = username;
        result.score = score;
        result.totalQuestions = totalQuestions;
        return result;
    }

    public Long getId() {
        return id;
    }

    public Long getSetId() {
        return setId;
    }

    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}