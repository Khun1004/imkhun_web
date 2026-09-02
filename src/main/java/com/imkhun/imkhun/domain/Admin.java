package com.imkhun.imkhun.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// USERS 테이블과 완전히 별개인 관리자 전용 테이블
@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String paymentInfo;

    // 로그인 유지용 토큰 — DB에 저장되니까 서버(devtools)가 재시작돼도 안 풀림
    @Column
    private String sessionToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Admin() {
        // JPA 기본 생성자
    }

    public static Admin create(String username, String encodedPassword, String email, String phone) {
        Admin admin = new Admin();
        admin.username = username;
        admin.password = encodedPassword;
        admin.email = email;
        admin.phone = phone;
        return admin;
    }

    public void updatePaymentInfo(String paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    public void updateSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPaymentInfo() {
        return paymentInfo;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}