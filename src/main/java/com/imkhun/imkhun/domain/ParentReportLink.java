package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생이 부모님께 공유하는 "리포트 링크". 로그인 없이 이 토큰만 있으면 출석/성적 요약을 볼 수 있음
@Entity
@Table(name = "parent_report_links")
public class ParentReportLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ParentReportLink() {
        // JPA 기본 생성자
    }

    public static ParentReportLink create(String token, String username) {
        ParentReportLink link = new ParentReportLink();
        link.token = token;
        link.username = username;
        return link;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}