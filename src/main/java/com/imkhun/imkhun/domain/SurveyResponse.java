package com.imkhun.imkhun.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// 학생 만족도 설문 — 리뷰(Review)보다 구조화된 형태로, 항목별 별점 + 익명 여부를 담음.
// 신청(Application) 하나당 한 번만 제출할 수 있음.
@Entity
@Table(name = "survey_responses")
public class SurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    // 제출한 학생 아이디 — 익명이어도 서버 내부적으로는 남겨둠(중복 제출 방지용). 관리자 화면에는 익명이면 안 보여줌
    @Column(nullable = false)
    private String username;

    @Column(name = "course_name", nullable = false)
    private String courseName;

    // 항목별 별점 1~5
    @Column(name = "content_rating", nullable = false)
    private int contentRating;

    @Column(name = "teacher_rating", nullable = false)
    private int teacherRating;

    @Column(name = "material_rating", nullable = false)
    private int materialRating;

    @Column(name = "overall_rating", nullable = false)
    private int overallRating;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String comment;

    @Column(name = "is_anonymous", nullable = false)
    private boolean isAnonymous;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected SurveyResponse() {
        // JPA 기본 생성자
    }

    public static SurveyResponse create(Long applicationId, String username, String courseName,
                                        int contentRating, int teacherRating, int materialRating,
                                        int overallRating, String comment, boolean isAnonymous) {
        SurveyResponse response = new SurveyResponse();
        response.applicationId = applicationId;
        response.username = username;
        response.courseName = courseName;
        response.contentRating = contentRating;
        response.teacherRating = teacherRating;
        response.materialRating = materialRating;
        response.overallRating = overallRating;
        response.comment = comment;
        response.isAnonymous = isAnonymous;
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getUsername() {
        return username;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getContentRating() {
        return contentRating;
    }

    public int getTeacherRating() {
        return teacherRating;
    }

    public int getMaterialRating() {
        return materialRating;
    }

    public int getOverallRating() {
        return overallRating;
    }

    public String getComment() {
        return comment;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}