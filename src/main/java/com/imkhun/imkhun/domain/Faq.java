package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 공개 홈페이지 "공지사항" 탭에 나오는 자주 묻는 질문
@Entity
@Table(name = "faqs")
public class Faq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String answer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Faq() {
        // JPA 기본 생성자
    }

    public static Faq create(String question, String answer) {
        Faq faq = new Faq();
        faq.question = question;
        faq.answer = answer;
        return faq;
    }

    public void update(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}