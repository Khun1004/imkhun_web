package com.imkhun.imkhun.dto;

public record GoalResponse(Long id, String type, String title, int targetValue, int currentProgress,
                           boolean achieved, String achievedAt, String createdAt) {
}