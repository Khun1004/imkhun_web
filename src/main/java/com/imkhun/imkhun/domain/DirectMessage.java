package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 선생님(관리자)과 학생 사이의 1:1 메시지. 학생 한 명당 하나의 대화방이고,
// senderType으로 누가 보낸 메시지인지 구분함 ("STUDENT" 또는 "ADMIN")
@Entity
@Table(name = "direct_messages")
public class DirectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 메시지가 속한 대화방 — 학생의 username (관리자가 보낸 메시지도 이 값으로 어느 학생 대화인지 구분함)
    @Column(nullable = false)
    private String username;

    // "STUDENT" 또는 "ADMIN"
    @Column(nullable = false)
    private String senderType;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 관리자가 이 메시지(학생이 보낸 것)를 읽었는지
    @Column(name = "read_by_admin", nullable = false)
    private boolean readByAdmin;

    // 학생이 이 메시지(관리자가 보낸 것)를 읽었는지
    @Column(name = "read_by_student", nullable = false)
    private boolean readByStudent;

    protected DirectMessage() {
        // JPA 기본 생성자
    }

    public static DirectMessage fromStudent(String username, String content) {
        DirectMessage message = new DirectMessage();
        message.username = username;
        message.senderType = "STUDENT";
        message.content = content;
        message.readByAdmin = false;
        message.readByStudent = true;
        return message;
    }

    public static DirectMessage fromAdmin(String username, String content) {
        DirectMessage message = new DirectMessage();
        message.username = username;
        message.senderType = "ADMIN";
        message.content = content;
        message.readByAdmin = true;
        message.readByStudent = false;
        return message;
    }

    public void markReadByAdmin() {
        this.readByAdmin = true;
    }

    public void markReadByStudent() {
        this.readByStudent = true;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSenderType() {
        return senderType;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isReadByAdmin() {
        return readByAdmin;
    }

    public boolean isReadByStudent() {
        return readByStudent;
    }
}