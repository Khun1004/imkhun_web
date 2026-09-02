package com.imkhun.imkhun.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작성자 아이디 (User.username) — 누가 썼는지 연결
    @Column(nullable = false)
    private String username;

    // 화면에 보여줄 작성자 닉네임 (작성 당시 닉네임을 스냅샷으로 저장)
    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private int rating;

    @Lob
    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 관리자(쿤)가 남긴 답글 — 없으면 null
    @Lob
    @Column(columnDefinition = "CLOB")
    private String adminReply;

    @Column
    private LocalDateTime repliedAt;

    protected Review() {
        // JPA 기본 생성자
    }

    public static Review create(String username, String nickname, String courseName, int rating, String content) {
        Review review = new Review();
        review.username = username;
        review.nickname = nickname;
        review.courseName = courseName;
        review.rating = rating;
        review.content = content;
        return review;
    }

    public void updateReply(String reply) {
        this.adminReply = reply;
        this.repliedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }
}