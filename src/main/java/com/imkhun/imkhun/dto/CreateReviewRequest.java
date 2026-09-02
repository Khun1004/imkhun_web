package com.imkhun.imkhun.dto;

public record CreateReviewRequest(String courseName, int rating, String content) {
}