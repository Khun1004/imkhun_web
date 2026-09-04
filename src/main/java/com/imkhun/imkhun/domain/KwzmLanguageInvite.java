package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// KWZM Center에서, 특정 언어(예: korean)의 자료 또는 영상을 볼 수 있도록 초대된 학생번호 목록
// 승인된 신청서가 있어도, 여기 초대돼있어야만 볼 수 있음. 자료로 공부하는 학생과 영상으로 공부하는
// 학생이 다를 수 있어서, contentType("MATERIAL" / "VIDEO")으로 완전히 따로 관리함
@Entity
@Table(name = "kwzm_language_invites", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"language", "content_type", "student_number"})
})
public class KwzmLanguageInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String language;

    // "MATERIAL" / "VIDEO"
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime invitedAt = LocalDateTime.now();

    protected KwzmLanguageInvite() {
        // JPA 기본 생성자
    }

    public static KwzmLanguageInvite create(String language, String contentType, String studentNumber) {
        KwzmLanguageInvite invite = new KwzmLanguageInvite();
        invite.language = language;
        invite.contentType = contentType;
        invite.studentNumber = studentNumber;
        return invite;
    }

    public Long getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }
}