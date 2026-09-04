package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 게시판 글의 댓글 — 학생이 쓴 댓글과 관리자가 쓴 댓글을 authorType으로 구분해요
@Entity
@Table(name = "post_comments")
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String nickname;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    // "STUDENT" / "ADMIN"
    @Column(name = "author_type", nullable = false)
    private String authorType = "STUDENT";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected PostComment() {
        // JPA 기본 생성자
    }

    public static PostComment create(Long postId, String username, String nickname, String content) {
        PostComment comment = new PostComment();
        comment.postId = postId;
        comment.username = username;
        comment.nickname = nickname;
        comment.content = content;
        comment.authorType = "STUDENT";
        return comment;
    }

    public static PostComment createByAdmin(Long postId, String adminUsername, String content) {
        PostComment comment = new PostComment();
        comment.postId = postId;
        comment.username = adminUsername;
        comment.nickname = "관리자";
        comment.content = content;
        comment.authorType = "ADMIN";
        return comment;
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getContent() {
        return content;
    }

    public boolean isAdmin() {
        return "ADMIN".equals(authorType);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}