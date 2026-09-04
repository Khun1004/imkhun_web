package com.imkhun.imkhun.dto;

public record AdminApplicationResponse(Long id, String nickname, String email, String studyType, String courseName,
                                       String contact, String memo, String status, String createdAt,
                                       String studentNumber) {
}