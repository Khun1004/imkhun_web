package com.imkhun.imkhun.dto;

// bestScore가 null이면 아직 이 Part 퀴즈를 한 번도 완료 안 한 것
public record QuizResultResponse(Integer bestScore, Integer totalQuestions, String completedAt) {
}