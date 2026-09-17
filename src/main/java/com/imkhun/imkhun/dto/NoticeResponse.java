package com.imkhun.imkhun.dto;

// scheduledAt: 예약 시간(없으면 null). isPublished: 지금 학생한테 보이는 상태인지
public record NoticeResponse(Long id, String title, String content, String createdAt,
                             String scheduledAt, boolean isPublished) {
}