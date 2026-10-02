package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 강의 자료(StudyMaterial) 하나에 달리는 질문 한 건. 같은 자료를 보는 다른 학생들도 이 질문/답변을
// 같이 볼 수 있음(익명 질문하기와는 다르게, 닉네임이 그대로 보임) — 그래서 같은 자료를 보는
// 친구들끼리 궁금한 점을 서로 참고할 수 있게 함.
@Entity
@Table(name = "material_questions")
public class MaterialQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(nullable = false)
    private String username;

    // 질문 당시 닉네임을 그대로 저장해둠(나중에 닉네임이 바뀌어도 이 질문에는 그대로 남음)
    @Column(nullable = false)
    private String nickname;

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

    protected MaterialQuestion() {
        // JPA 기본 생성자
    }

    public static MaterialQuestion create(Long materialId, String username, String nickname, String questionText) {
        MaterialQuestion question = new MaterialQuestion();
        question.materialId = materialId;
        question.username = username;
        question.nickname = nickname;
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

    public Long getMaterialId() {
        return materialId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
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