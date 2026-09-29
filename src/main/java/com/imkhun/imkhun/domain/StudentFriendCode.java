package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생 한 명당 하나씩 가지는 "친구 추가용" 초대 코드. 친구한테 이 코드를 알려주면
// 상대방이 코드를 입력해서 서로 친구가 됨
@Entity
@Table(name = "student_friend_codes")
public class StudentFriendCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudentFriendCode() {
        // JPA 기본 생성자
    }

    public static StudentFriendCode create(String username, String code) {
        StudentFriendCode entity = new StudentFriendCode();
        entity.username = username;
        entity.code = code;
        return entity;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}