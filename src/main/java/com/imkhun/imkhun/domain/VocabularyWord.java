package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 선생님이 등록해두는 단어 하나. 언어별로 나눠서 관리하고, 학생은 언어를 골라서 플래시카드로 봄
@Entity
@Table(name = "vocabulary_words")
public class VocabularyWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "korean" / "japanese" / "thai" / "english" 등 — 다른 자료(StudyMaterial)에서 쓰는 것과 같은 자유 문자열
    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String meaning;

    // 예문 (선택)
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String example;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected VocabularyWord() {
        // JPA 기본 생성자
    }

    public static VocabularyWord create(String language, String word, String meaning, String example) {
        VocabularyWord vocabularyWord = new VocabularyWord();
        vocabularyWord.language = language;
        vocabularyWord.word = word;
        vocabularyWord.meaning = meaning;
        vocabularyWord.example = example;
        return vocabularyWord;
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getExample() {
        return example;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}