package com.imkhun.imkhun.dto;

// studentUsername은 익명이면 null로 나감
public record SurveyResponseDetail(Long id, String studentUsername, String courseName,
                                   int contentRating, int teacherRating, int materialRating,
                                   int overallRating, String comment, boolean isAnonymous, String createdAt) {
}