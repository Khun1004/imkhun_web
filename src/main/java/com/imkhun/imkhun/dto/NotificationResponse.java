package com.imkhun.imkhun.dto;

public record NotificationResponse(Long id, String type, String message, Long postId, boolean isRead, String createdAt) {
}