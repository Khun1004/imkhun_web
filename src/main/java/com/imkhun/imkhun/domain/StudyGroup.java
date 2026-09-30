package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 여러 친구가 모인 스터디 그룹 하나. 만든 사람이 친구들을 초대해서 시작함
@Entity
@Table(name = "study_groups")
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "creator_username", nullable = false)
    private String creatorUsername;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StudyGroup() {
        // JPA 기본 생성자
    }

    public static StudyGroup create(String name, String creatorUsername) {
        StudyGroup group = new StudyGroup();
        group.name = name;
        group.creatorUsername = creatorUsername;
        return group;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}