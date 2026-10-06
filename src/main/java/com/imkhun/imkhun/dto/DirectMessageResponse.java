package com.imkhun.imkhun.dto;

// 메시지 한 개 — senderType은 "STUDENT" 또는 "ADMIN"
public record DirectMessageResponse(Long id, String senderType, String content, String createdAt) {
}