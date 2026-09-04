package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 게시판 글의 좋아요/싫어요 — 학생 한 명당 글 하나에 반응 하나만 (좋아요<->싫어요 전환 가능)
@Entity
@Table(name = "post_reactions", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "username"}))
public class PostReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false)
    private String username;

    // "LIKE" / "DISLIKE"
    @Column(nullable = false)
    private String type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected PostReaction() {
        // JPA 기본 생성자
    }

    public static PostReaction create(Long postId, String username, String type) {
        PostReaction reaction = new PostReaction();
        reaction.postId = postId;
        reaction.username = username;
        reaction.type = type;
        return reaction;
    }

    public void changeType(String type) {
        this.type = type;
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

    public String getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}