package com.imkhun.imkhun.dto;

public record VoiceSubmissionResponse(Long id, String studentUsername, String courseName, String title,
                                      String audioData, String adminComment, String createdAt) {
}