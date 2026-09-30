package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 스터디 그룹의 멤버 한 명. status: INVITED(초대됨, 수락 대기) / ACTIVE(참여중) / DECLINED(거절함) / LEFT(나감)
@Entity
@Table(name = "study_group_members")
public class StudyGroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String status;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt = LocalDateTime.now();

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    protected StudyGroupMember() {
        // JPA 기본 생성자
    }

    // 그룹을 처음 만든 사람은 바로 ACTIVE로 시작함
    public static StudyGroupMember createActive(Long groupId, String username) {
        StudyGroupMember member = new StudyGroupMember();
        member.groupId = groupId;
        member.username = username;
        member.status = "ACTIVE";
        member.joinedAt = LocalDateTime.now();
        return member;
    }

    public static StudyGroupMember createInvited(Long groupId, String username) {
        StudyGroupMember member = new StudyGroupMember();
        member.groupId = groupId;
        member.username = username;
        member.status = "INVITED";
        return member;
    }

    public void accept() {
        this.status = "ACTIVE";
        this.joinedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = "DECLINED";
    }

    public void leave() {
        this.status = "LEFT";
    }

    // 예전에 거절했거나 나간 사람을 다시 초대할 때 같은 줄을 재사용함
    public void resetToInvited() {
        this.status = "INVITED";
        this.invitedAt = LocalDateTime.now();
        this.joinedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}