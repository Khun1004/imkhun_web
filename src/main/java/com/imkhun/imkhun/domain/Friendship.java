package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생 두 명 사이의 친구 관계 한 건. usernameA/usernameB는 항상 사전순으로 작은 쪽이 A가 되도록
// 저장해서, "누가 먼저 추가했는지"와 상관없이 같은 관계가 중복 저장되지 않게 함
@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(columnNames = {"username_a", "username_b"}))
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username_a", nullable = false)
    private String usernameA;

    @Column(name = "username_b", nullable = false)
    private String usernameB;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Friendship() {
        // JPA 기본 생성자
    }

    public static Friendship create(String usernameOne, String usernameTwo) {
        Friendship friendship = new Friendship();
        if (usernameOne.compareTo(usernameTwo) <= 0) {
            friendship.usernameA = usernameOne;
            friendship.usernameB = usernameTwo;
        } else {
            friendship.usernameA = usernameTwo;
            friendship.usernameB = usernameOne;
        }
        return friendship;
    }

    public Long getId() {
        return id;
    }

    public String getUsernameA() {
        return usernameA;
    }

    public String getUsernameB() {
        return usernameB;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}