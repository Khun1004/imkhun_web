package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// KWZM Center에서, 특정 언어(예: korean)의 자료를 볼 수 있도록 초대된 학생번호 목록
// 승인된 신청서가 있어도, 여기 초대돼있어야만 KWZM 자료를 볼 수 있음
@Entity
@Table(name = "kwzm_language_invites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"language", "student_number"})
})
public class KwzmLanguageInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String language;

    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime invitedAt = LocalDateTime.now();

    protected KwzmLanguageInvite() {
        // JPA 기본 생성자
    }

    public static KwzmLanguageInvite create(String language, String studentNumber) {
        KwzmLanguageInvite invite = new KwzmLanguageInvite();
        invite.language = language;
        invite.studentNumber = studentNumber;
        return invite;
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }
}