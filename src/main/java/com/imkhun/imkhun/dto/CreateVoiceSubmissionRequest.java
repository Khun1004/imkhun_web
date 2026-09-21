package com.imkhun.imkhun.dto;

// audioData는 "data:audio/webm;base64,...." 형식의 데이터 URI
public record CreateVoiceSubmissionRequest(Long applicationId, String title, String audioData) {
}