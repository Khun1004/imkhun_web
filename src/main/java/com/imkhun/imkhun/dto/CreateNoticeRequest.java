package com.imkhun.imkhun.dto;

// scheduledAt은 "2026-09-20T09:00" 형식 문자열이거나 비워두면 바로 공개됨
public record CreateNoticeRequest(String title, String content, String scheduledAt) {
}