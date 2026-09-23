package com.imkhun.imkhun.dto;

// 관리자용 — 절대로 username이 안 들어감 (누가 썼는지 알 수 없게)
public record AdminQuestionResponse(Long id, String questionText, String answerText, String createdAt, String answeredAt) {
}