package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 단어 하나. 어떤 Part(VocabularySet)에 속하는지 setId로 연결됨
@Entity
@Table(name = "vocabulary_words")
public class VocabularyWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_id", nullable = false)
    private Long setId;

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

    public static VocabularyWord create(Long setId, String word, String meaning, String example) {
        VocabularyWord vocabularyWord = new VocabularyWord();
        vocabularyWord.setId = setId;
        vocabularyWord.word = word;
        vocabularyWord.meaning = meaning;
        vocabularyWord.example = example;
        return vocabularyWord;
    }

    public Long getId() {
        return id;
    }

    public Long getSetId() {
        return setId;
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