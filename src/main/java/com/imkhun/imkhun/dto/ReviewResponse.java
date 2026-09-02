package com.imkhun.imkhun.dto;

public record ReviewResponse(Long id, String nickname, String profileImage, String courseName,
                             int rating, String content, String createdAt,
                             String adminReply, String repliedAt) {
}