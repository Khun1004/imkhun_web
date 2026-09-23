package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 익명으로 보내는 질문. username은 학생이 "내가 보낸 질문"을 다시 볼 수 있게 내부적으로만 저장하고,
// 관리자한테 보여줄 때는 절대 누구인지 안 알려줌
@Entity
@Table(name = "student_questions")
public class StudentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Lob
    @Column(name = "question_text", nullable = false, columnDefinition = "LONGTEXT")
    private String questionText;

    @Lob
    @Column(name = "answer_text", columnDefinition = "LONGTEXT")
    private String answerText;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudentQuestion() {
        // JPA 기본 생성자
    }

    public static StudentQuestion create(String username, String questionText) {
        StudentQuestion question = new StudentQuestion();
        question.username = username;
        question.questionText = questionText;
        return question;
    }

    public void addAnswer(String answerText) {
        this.answerText = answerText;
        this.answeredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getAnswerText() {
        return answerText;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}