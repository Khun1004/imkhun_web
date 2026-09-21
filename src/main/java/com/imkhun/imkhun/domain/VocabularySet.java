package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 단어장 묶음 하나 (예: "Part 1"). 이 안에 여러 단어(VocabularyWord)가 들어감
@Entity
@Table(name = "vocabulary_sets")
public class VocabularySet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "korean" / "japanese" / "thai" / "english" — 학생이 신청한 언어랑 매칭시켜서 걸러줄 때 씀
    @Column(nullable = false)
    private String language;

    // "Part 1" 같은 이름
    @Column(nullable = false)
    private String name;

    // "한국어 어휘" / "한국어 문장" 같은 자유 텍스트 — 화면에 라벨처럼 보여줌
    @Column(nullable = false)
    private String category;

    // 이 Part의 퀴즈를 몇 분 안에 풀어야 하는지
    @Column(name = "quiz_time_limit_minutes", nullable = false)
    private int quizTimeLimitMinutes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected VocabularySet() {
        // JPA 기본 생성자
    }

    public static VocabularySet create(String language, String name, String category, int quizTimeLimitMinutes) {
        VocabularySet set = new VocabularySet();
        set.language = language;
        set.name = name;
        set.category = category;
        set.quizTimeLimitMinutes = quizTimeLimitMinutes;
        return set;
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getQuizTimeLimitMinutes() {
        return quizTimeLimitMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}