package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 게시판 글 — 학생들이 언어별 어휘/문법/쓰기나 컴퓨터 관련 내용을 서로 올리는 공간
@Entity
@Table(name = "study_posts")
public class StudyPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "korean" / "japanese" / "thai" / "english" / "computer"
    @Column(nullable = false)
    private String topic;

    // "VOCAB" / "GRAMMAR" / "WRITING" / "OTHER" — topic이 "computer"이면 null (항목 구분 없음)
    private String category;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    // 작성한 학생의 로그인 아이디 — 나중에 본인 글만 수정/삭제하게 할 때 대비
    @Column(nullable = false)
    private String username;

    // 목록/상세에 보여줄 닉네임 (작성 시점 스냅샷)
    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudyPost() {
        // JPA 기본 생성자
    }

    public static StudyPost create(String topic, String category, String title, String content,
                                   String username, String nickname) {
        StudyPost post = new StudyPost();
        post.topic = topic;
        post.category = category;
        post.title = title;
        post.content = content;
        post.username = username;
        post.nickname = nickname;
        return post;
    }

    public void update(String category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}