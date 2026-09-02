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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // 사이트에서 로그인할 때 쓰는 아이디 (구글 이메일과는 별개)
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    // 관리자 로그인 시 본인 확인용 (마이페이지에서 등록/변경)
    @Column
    private String phone;

    // 프로필 사진 — data:image/...;base64,... 형태로 그대로 저장 (학습 단계라 단순하게)
    @Lob
    @Column(columnDefinition = "CLOB")
    private String profileImage;

    // 지금은 항상 "google" (구글로 본인 확인 후 가입)
    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // KWZM Center 로그인 유지용 토큰 — 관리자 로그인과 같은 방식 (스프링시큐리티 세션과 별개)
    @Column
    private String studentSessionToken;

    protected User() {
        // JPA가 사용하는 기본 생성자
    }

    public static User create(String email, String nickname, String username, String encodedPassword, String provider) {
        User user = new User();
        user.email = email;
        user.nickname = nickname;
        user.username = username;
        user.password = encodedPassword;
        user.provider = provider;
        return user;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void changePhone(String phone) {
        this.phone = phone;
    }

    public void updateStudentSessionToken(String token) {
        this.studentSessionToken = token;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getPhone() {
        return phone;
    }

    public String getProvider() {
        return provider;
    }

    public String getStudentSessionToken() {
        return studentSessionToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}