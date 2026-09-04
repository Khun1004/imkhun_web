package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 댓글의 좋아요/싫어요 — 학생, 관리자 모두 반응할 수 있어요 (한 사람당 댓글 하나에 반응 하나만)
@Entity
@Table(name = "comment_reactions", uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "username"}))
public class CommentReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    // 학생이면 학생 username, 관리자면 관리자 username
    @Column(nullable = false)
    private String username;

    // "LIKE" / "DISLIKE"
    @Column(nullable = false)
    private String type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected CommentReaction() {
        // JPA 기본 생성자
    }

    public static CommentReaction create(Long commentId, String username, String type) {
        CommentReaction reaction = new CommentReaction();
        reaction.commentId = commentId;
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

    public Long getCommentId() {
        return commentId;
    }

    public String getUsername() {
        return username;
    }

    public String getType() {
        return type;
    }
}