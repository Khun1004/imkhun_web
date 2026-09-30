package com.imkhun.imkhun.dto;

// examStartedAt / examCompletedAt은 category가 "EXAM"인 파일에서만 쓰여요.
// 둘 다 JS의 new Date()가 그대로 읽을 수 있게 ISO 형식 문자열로 내려줘요 (없으면 null)
public record SentFileResponse(Long id, String category, String fileName, String fileData,
                               String courseName, String createdAt,
                               String examStartedAt, String examCompletedAt) {
}