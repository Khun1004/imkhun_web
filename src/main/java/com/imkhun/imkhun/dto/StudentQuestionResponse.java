package com.imkhun.imkhun.dto;

// 학생 본인이 자기 질문을 볼 때 씀 (본인 거니까 당연히 자기 글인 건 알지만, DTO 자체엔 username 안 넣음)
public record StudentQuestionResponse(Long id, String questionText, String answerText, String createdAt, String answeredAt) {
}