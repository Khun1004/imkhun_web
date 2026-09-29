package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 친구끼리 서로 보내는 짧은 응원 쪽지 한 건
@Entity
@Table(name = "friend_notes")
public class FriendNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderUsername;

    @Column(nullable = false)
    private String receiverUsername;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected FriendNote() {
        // JPA 기본 생성자
    }

    public static FriendNote create(String senderUsername, String receiverUsername, String message) {
        FriendNote note = new FriendNote();
        note.senderUsername = senderUsername;
        note.receiverUsername = receiverUsername;
        note.message = message;
        return note;
    }

    public Long getId() {
        return id;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}